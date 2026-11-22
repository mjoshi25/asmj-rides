package in.asmj.ride.service;

import in.asmj.ride.model.AgentVehicleAuthorization;
import in.asmj.ride.model.TravelAgentProfile;
import in.asmj.ride.model.UserAccount;
import in.asmj.ride.model.Vehicle;
import in.asmj.ride.repo.AgentVehicleAuthorizationRepository;
import in.asmj.ride.repo.TravelAgentProfileRepository;
import in.asmj.ride.repo.UserAccountRepository;
import in.asmj.ride.repo.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service
public class AgentVehicleAuthorizationService {
    public static final Set<String> ALLOWED_PERMISSIONS = Set.of(
            "VIEW_LISTING", "MANAGE_LISTING", "MANAGE_ENQUIRIES", "MANAGE_BOOKINGS", "MANAGE_PRICING", "MANAGE_AVAILABILITY", "ASSIGN_DRIVER");
    private final AgentVehicleAuthorizationRepository authorizations;
    private final VehicleRepository vehicles;
    private final UserAccountRepository users;
    private final TravelAgentProfileRepository profiles;

    public AgentVehicleAuthorizationService(AgentVehicleAuthorizationRepository a, VehicleRepository v,
            UserAccountRepository u, TravelAgentProfileRepository p) {
        this.authorizations=a; this.vehicles=v; this.users=u; this.profiles=p;
    }

    public AgentVehicleAuthorization request(String agentId, String vehicleId, List<String> permissions) {
        UserAccount agent=users.findById(agentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Vehicle vehicle=vehicles.findById(vehicleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        TravelAgentProfile profile=profiles.findByUserId(agentId).orElse(null);
        if (!agent.getGrantedRoles().contains("TRAVEL_AGENT") || !"APPROVED".equals(agent.getStatus()) || !agent.isMobileVerified()
                || profile==null || !profile.isPartnerFleetEnabled() || !"APPROVED".equals(profile.getVerificationStatus()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Approved travel-agent profile required");
        if (agentId.equals(vehicle.getOwnerId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your own vehicle does not need partner authorization");
        if (!"APPROVED".equals(vehicle.getStatus()) || !"ACTIVE".equals(vehicle.getOperationalStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only approved active vehicles can be shared with an agent");
        if (vehicle.getOwnerId()==null || vehicle.getOwnerId().isBlank()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle owner is missing");
        UserAccount owner=users.findById(vehicle.getOwnerId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle owner account is missing"));
        if (!owner.getGrantedRoles().contains("FLEET_OWNER") || !"APPROVED".equals(owner.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle owner is not approved");
        List<String> clean=permissions==null?List.of("VIEW_LISTING","MANAGE_ENQUIRIES"):permissions.stream().filter(ALLOWED_PERMISSIONS::contains).distinct().toList();
        if (clean.isEmpty() || !clean.contains("VIEW_LISTING")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VIEW_LISTING permission is required");
        Optional<AgentVehicleAuthorization> existing=authorizations.findByOwnerIdAndAgentIdAndVehicleId(owner.getId(),agentId,vehicleId);
        AgentVehicleAuthorization item=existing.orElseGet(AgentVehicleAuthorization::new);
        if (existing.isPresent() && "APPROVED".equals(item.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Authorization is already active");
        if (existing.isPresent() && "PENDING".equals(item.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "An authorization request is already pending");
        item.setOwnerId(owner.getId()); item.setAgentId(agentId); item.setVehicleId(vehicleId);
        item.setPermissions(clean); item.setStatus("PENDING"); item.setRequestedAt(Instant.now());
        item.setDecidedAt(null); item.setDecidedBy(null);
        return authorizations.save(item);
    }

    public AgentVehicleAuthorization decide(String ownerId, String authorizationId, String decision, List<String> permissions) {
        AgentVehicleAuthorization item=authorizations.findById(authorizationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ownerId.equals(item.getOwnerId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (!"PENDING".equals(item.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending requests can be decided");
        if ("APPROVE".equals(decision)) {
            Vehicle vehicle=vehicles.findById(item.getVehicleId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle no longer exists"));
            UserAccount agent=users.findById(item.getAgentId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Agent account no longer exists"));
            TravelAgentProfile profile=profiles.findByUserId(item.getAgentId()).orElse(null);
            if (!ownerId.equals(vehicle.getOwnerId()) || !"APPROVED".equals(vehicle.getStatus()) || !"ACTIVE".equals(vehicle.getOperationalStatus())
                    || !"APPROVED".equals(agent.getStatus()) || !agent.isMobileVerified() || profile==null || !"APPROVED".equals(profile.getVerificationStatus()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Owner, agent or vehicle eligibility changed; refresh the request before approval");
            List<String> clean=permissions==null?item.getPermissions():permissions.stream().filter(ALLOWED_PERMISSIONS::contains).distinct().toList();
            if (clean.isEmpty() || !clean.contains("VIEW_LISTING")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VIEW_LISTING permission is required");
            item.setPermissions(clean); item.setStatus("APPROVED");
        } else if ("REJECT".equals(decision)) item.setStatus("REJECTED");
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid decision");
        item.setDecidedBy(ownerId); item.setDecidedAt(Instant.now());
        return authorizations.save(item);
    }

    public AgentVehicleAuthorization revoke(String ownerId, String authorizationId) {
        AgentVehicleAuthorization item=authorizations.findById(authorizationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ownerId.equals(item.getOwnerId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (!"APPROVED".equals(item.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active authorizations can be revoked");
        item.setStatus("REVOKED"); item.setDecidedBy(ownerId); item.setDecidedAt(Instant.now());
        return authorizations.save(item);
    }

    public boolean canManage(String agentId, String vehicleId, String permission) {
        if (!ALLOWED_PERMISSIONS.contains(permission)) return false;
        UserAccount agent=users.findById(agentId).orElse(null);
        Vehicle vehicle=vehicles.findById(vehicleId).orElse(null);
        TravelAgentProfile profile=profiles.findByUserId(agentId).orElse(null);
        UserAccount owner=vehicle==null || vehicle.getOwnerId()==null ? null : users.findById(vehicle.getOwnerId()).orElse(null);
        if (agent==null || vehicle==null || owner==null || !agent.getGrantedRoles().contains("TRAVEL_AGENT")
                || !"APPROVED".equals(agent.getStatus()) || !agent.isMobileVerified()
                || profile==null || !"APPROVED".equals(profile.getVerificationStatus())
                || !owner.getGrantedRoles().contains("FLEET_OWNER") || !"APPROVED".equals(owner.getStatus())
                || !"APPROVED".equals(vehicle.getStatus()) || !"ACTIVE".equals(vehicle.getOperationalStatus())) return false;
        return authorizations.findByAgentIdAndVehicleIdAndStatus(agentId, vehicleId, "APPROVED").stream()
                .anyMatch(a -> vehicle.getOwnerId().equals(a.getOwnerId()) && a.getPermissions().contains(permission));
    }
}
