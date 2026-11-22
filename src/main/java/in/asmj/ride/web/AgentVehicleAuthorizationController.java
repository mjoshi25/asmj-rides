package in.asmj.ride.web;

import in.asmj.ride.model.AgentVehicleAuthorization;
import in.asmj.ride.model.UserAccount;
import in.asmj.ride.model.Vehicle;
import in.asmj.ride.repo.AgentVehicleAuthorizationRepository;
import in.asmj.ride.repo.UserAccountRepository;
import in.asmj.ride.repo.VehicleRepository;
import in.asmj.ride.service.AgentVehicleAuthorizationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
public class AgentVehicleAuthorizationController {
    private final UserAccountRepository users;
    private final VehicleRepository vehicles;
    private final AgentVehicleAuthorizationRepository authorizations;
    private final AgentVehicleAuthorizationService service;

    public AgentVehicleAuthorizationController(UserAccountRepository users, VehicleRepository vehicles,
            AgentVehicleAuthorizationRepository authorizations, AgentVehicleAuthorizationService service) {
        this.users=users; this.vehicles=vehicles; this.authorizations=authorizations; this.service=service;
    }

    private UserAccount current(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
    }

    @GetMapping("/agent/authorizations")
    public String agentAuthorizations(Authentication authentication, Model model) {
        UserAccount agent=current(authentication);
        model.addAttribute("viewer",agent);
        model.addAttribute("agentView",true);
        model.addAttribute("authorizations",authorizations.findByAgentIdOrderByRequestedAtDesc(agent.getId()));
        model.addAttribute("vehicles",vehicles.findByStatus("APPROVED").stream()
                .filter(v -> "ACTIVE".equals(v.getOperationalStatus()) && v.getOwnerId()!=null && !agent.getId().equals(v.getOwnerId()))
                .filter(v -> users.findById(v.getOwnerId()).map(owner -> owner.getGrantedRoles().contains("FLEET_OWNER") && "APPROVED".equals(owner.getStatus())).orElse(false))
                .filter(v -> authorizations.findByOwnerIdAndAgentIdAndVehicleId(v.getOwnerId(),agent.getId(),v.getId())
                        .map(existing -> !List.of("PENDING","APPROVED").contains(existing.getStatus())).orElse(true)).toList());
        model.addAttribute("owners",users.findAll().stream().filter(u -> u.hasGrantedRole("FLEET_OWNER")).toList());
        model.addAttribute("ownerNames",users.findAll().stream().collect(java.util.stream.Collectors.toMap(UserAccount::getId,UserAccount::getName,(a,b)->a)));
        return "agent-authorizations";
    }

    @GetMapping("/fleet/authorizations")
    public String ownerAuthorizations(Authentication authentication, Model model) {
        UserAccount owner=current(authentication);
        model.addAttribute("viewer",owner);
        model.addAttribute("agentView",false);
        model.addAttribute("authorizations",authorizations.findByOwnerIdOrderByRequestedAtDesc(owner.getId()));
        model.addAttribute("vehicles",vehicles.findByOwnerId(owner.getId()));
        model.addAttribute("agentNames",users.findAll().stream().collect(java.util.stream.Collectors.toMap(
                UserAccount::getId, UserAccount::getName, (a,b)->a)));
        return "agent-authorizations";
    }

    @PostMapping("/agent/authorizations/request")
    public String request(Authentication authentication, @RequestParam String vehicleId,
            @RequestParam(required=false) List<String> permissions) {
        UserAccount agent=current(authentication);
        service.request(agent.getId(),vehicleId,permissions);
        return "redirect:/agent/authorizations?requested";
    }

    @PostMapping("/fleet/authorizations/{id}/decision")
    public String decide(Authentication authentication, @PathVariable String id,
            @RequestParam String decision, @RequestParam(required=false) List<String> permissions) {
        UserAccount owner=current(authentication);
        service.decide(owner.getId(),id,decision,permissions);
        return "redirect:/fleet/authorizations?updated";
    }

    @PostMapping("/fleet/authorizations/{id}/revoke")
    public String revoke(Authentication authentication, @PathVariable String id) {
        UserAccount owner=current(authentication);
        service.revoke(owner.getId(),id);
        return "redirect:/fleet/authorizations?revoked";
    }
}
