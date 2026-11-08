package in.asmj.ride.web;

import in.asmj.ride.model.*;
import in.asmj.ride.repo.*;
import in.asmj.ride.service.DocumentRequirementService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.*;
import java.time.*;
import in.asmj.ride.service.RideNotificationService;
import in.asmj.ride.service.VehicleReservationLockService;
import java.util.stream.Collectors;
import java.security.SecureRandom;

@Controller
public class FleetController {
    private final UserAccountRepository users;
    private final VehicleRepository vehicles;
    private final RideBookingRepository rides;
    private final PaymentRecordRepository payments;
    private final RideNotificationService notifications;
    private final VehicleReservationLockService reservationLocks;
    private final SecureRandom random=new SecureRandom();
    private final PasswordEncoder passwordEncoder;
    private final DocumentRequirementService documentRequirements;

    public FleetController(UserAccountRepository users, VehicleRepository vehicles,
                           RideBookingRepository rides, PaymentRecordRepository payments,
                           RideNotificationService notifications, VehicleReservationLockService reservationLocks, PasswordEncoder passwordEncoder, DocumentRequirementService documentRequirements) {
        this.users=users; this.vehicles=vehicles; this.rides=rides; this.payments=payments; this.notifications=notifications; this.reservationLocks=reservationLocks; this.passwordEncoder=passwordEncoder; this.documentRequirements=documentRequirements;
    }

    @GetMapping("/fleet")
    public String dashboard(Authentication authentication, Model model) {
        UserAccount owner=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        List<UserAccount> drivers=users.findByRoleIn(List.of("DRIVER")).stream()
                .filter(d->owner.getId().equals(d.getFleetOwnerId())).toList();
        Set<String> driverIds=drivers.stream().map(UserAccount::getId).collect(Collectors.toSet());
        List<Vehicle> fleetVehicles=vehicles.findByOwnerId(owner.getId());
        Set<String> vehicleIds=fleetVehicles.stream().map(Vehicle::getId).collect(Collectors.toSet());
        List<RideBooking> fleetRides=rides.findAll().stream().filter(r ->
                driverIds.contains(r.getAssignedDriverId()) || driverIds.contains(r.getReturnAssignedDriverId()) ||
                (r.getAssignedVehicleId()!=null && vehicleIds.contains(r.getAssignedVehicleId())) ||
                (r.getRequestedVehicleId()!=null && vehicleIds.contains(r.getRequestedVehicleId())) ||
                (r.getReturnAssignedVehicleId()!=null && vehicleIds.contains(r.getReturnAssignedVehicleId())) ||
                ("REQUESTED".equals(r.getStatus()) && fleetVehicles.stream().anyMatch(v -> "APPROVED".equals(v.getStatus()) && Objects.equals(v.getCategory(),r.getCategory()) && (v.getBaseCityId()==null || v.getBaseCityId().isBlank() || Objects.equals(v.getBaseCityId(),r.getPickupCityId()))) && (r.getRequestedVehicleId()==null || vehicleIds.contains(r.getRequestedVehicleId()))))
                .sorted(Comparator.comparing(RideBooking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        List<PaymentRecord> fleetPayments=payments.findAll().stream().filter(p ->
                owner.getId().equals(p.getFleetOwnerId()) || owner.getId().equals(p.getReturnFleetOwnerId())).toList();
        model.addAttribute("owner",owner);
        model.addAttribute("selfDriverEnabled",owner.isFleetOwnerDriverEnabled());
        model.addAttribute("drivers",drivers);
        model.addAttribute("pendingDriverAssociations",users.findByRoleIn(List.of("DRIVER")).stream().filter(d->owner.getId().equals(d.getPendingFleetOwnerId()) && "PENDING".equals(d.getFleetAssociationStatus())).toList());
        model.addAttribute("vehicles",fleetVehicles);
        model.addAttribute("rides",fleetRides);
        model.addAttribute("payments",fleetPayments);
        model.addAttribute("pendingPayments",fleetPayments.stream().filter(p->!List.of("PAID","REFUND_PROCESSED").contains(p.getStatus())).count());
        List<Map<String,Object>> driverStats=new ArrayList<>();
        for(UserAccount d:drivers){Map<String,Object> stat=new LinkedHashMap<>();stat.put("name",d.getName());stat.put("mobile",d.getMobile());stat.put("status",d.getStatus());stat.put("available",d.isAvailable());stat.put("tripCount",fleetRides.stream().filter(r->d.getId().equals(r.getAssignedDriverId())||d.getId().equals(r.getReturnAssignedDriverId())).count());double net=fleetPayments.stream().filter(p->d.getId().equals(p.getDriverId())||d.getId().equals(p.getReturnDriverId())).filter(p->"PAID".equals(p.getStatus())).mapToDouble(p->(d.getId().equals(p.getDriverId())?p.getDriverNetAmount():0)+(d.getId().equals(p.getReturnDriverId())?p.getReturnDriverNetAmount():0)).sum();stat.put("netPaid",net);driverStats.add(stat);}
        model.addAttribute("driverStats",driverStats);
        List<Map<String,Object>> vehicleStats=new ArrayList<>();
        for(Vehicle v:fleetVehicles){Map<String,Object> stat=new LinkedHashMap<>();stat.put("registration",v.getRegistrationNumber());stat.put("category",v.getCategory());stat.put("status",v.getStatus());long tripCount=fleetRides.stream().filter(r->v.getId().equals(r.getAssignedVehicleId())||v.getId().equals(r.getReturnAssignedVehicleId())).count();stat.put("tripCount",tripCount);double allocated=fleetPayments.stream().filter(p->"PAID".equals(p.getStatus())).mapToDouble(p->{RideBooking r=fleetRides.stream().filter(x->x.getId().equals(p.getRideId())).findFirst().orElse(null);if(r==null)return 0;return (v.getId().equals(r.getAssignedVehicleId())?p.getOutwardGrossAmount():0)+(v.getId().equals(r.getReturnAssignedVehicleId())?p.getReturnGrossAmount():0);}).sum();stat.put("allocatedGross",allocated);vehicleStats.add(stat);}
        model.addAttribute("vehicleStats",vehicleStats);
        model.addAttribute("activeDrivers",drivers.stream().filter(UserAccount::isAvailable).count());
        model.addAttribute("approvedVehicles",fleetVehicles.stream().filter(v->"APPROVED".equals(v.getStatus())).count());
        model.addAttribute("activeTrips",fleetRides.stream().filter(r->List.of("ACCEPTED","ASSIGNED","IN_PROGRESS").contains(r.getStatus()) || ("ACCEPTED".equals(r.getReturnLegStatus())||"IN_PROGRESS".equals(r.getReturnLegStatus()))).count());
        model.addAttribute("completedTrips",fleetRides.stream().filter(r->"COMPLETED".equals(r.getStatus())).count());
        model.addAttribute("gross",fleetPayments.stream().filter(p->"PAID".equals(p.getStatus())).mapToDouble(p ->
                (owner.getId().equals(p.getFleetOwnerId())?(p.getOutwardGrossAmount()>0?p.getOutwardGrossAmount():p.getGrossAmount()):0) +
                (owner.getId().equals(p.getReturnFleetOwnerId())?p.getReturnGrossAmount():0)).sum());
        model.addAttribute("commission",fleetPayments.stream().filter(p->"PAID".equals(p.getStatus())).mapToDouble(p ->
                (owner.getId().equals(p.getFleetOwnerId())?p.getCommissionAmount():0) +
                (owner.getId().equals(p.getReturnFleetOwnerId())?p.getReturnCommissionAmount():0)).sum());
        model.addAttribute("unsettled",fleetPayments.stream().filter(p->"PAID".equals(p.getStatus())).mapToDouble(p ->
                (owner.getId().equals(p.getFleetOwnerId()) && !p.isSettled()?p.getDriverNetAmount():0) +
                (owner.getId().equals(p.getReturnFleetOwnerId()) && !p.isReturnSettled()?p.getReturnDriverNetAmount():0)).sum());
        model.addAttribute("settled",fleetPayments.stream().filter(p->"PAID".equals(p.getStatus())).mapToDouble(p ->
                (owner.getId().equals(p.getFleetOwnerId()) && p.isSettled()?p.getDriverNetAmount():0) +
                (owner.getId().equals(p.getReturnFleetOwnerId()) && p.isReturnSettled()?p.getReturnDriverNetAmount():0)).sum());
        return "fleet";
    }

    @PostMapping("/fleet/self-driver")
    public String toggleSelfDriver(Authentication authentication, @RequestParam(defaultValue="false") boolean enabled) {
        UserAccount owner=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        if(!"FLEET_OWNER".equals(owner.getRole()) || !"APPROVED".equals(owner.getStatus()) || !owner.isMobileVerified()) return "redirect:/fleet?error=selfDriver";
        boolean licenceVerified=owner.getDocuments()!=null && owner.getDocuments().stream().anyMatch(d->"DRIVING_LICENCE".equals(d.getCategory()) && "VERIFIED".equals(d.getVerificationStatus()));
        if(enabled && (!documentRequirements.verifiedUserDocuments(owner) || !licenceVerified)) return "redirect:/fleet?error=selfDriverLicence";
        owner.setFleetOwnerDriverEnabled(enabled); owner.setAvailable(enabled); users.save(owner);
        return "redirect:/fleet?selfDriverUpdated";
    }

    @PostMapping("/fleet/drivers/create")
    public String createFleetDriver(Authentication authentication, @RequestParam String name,
                                    @RequestParam String email, @RequestParam String mobile,
                                    @RequestParam String password) {
        UserAccount owner=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        String normalizedEmail=email==null?"":email.trim().toLowerCase(Locale.ROOT);
        String normalizedMobile=mobile==null?"":mobile.replaceAll("\\D", "");
        if(!"FLEET_OWNER".equals(owner.getRole()) || !"APPROVED".equals(owner.getStatus()) || !owner.isMobileVerified()
                || name==null || name.isBlank() || !normalizedEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
                || !normalizedMobile.matches("[0-9]{10}") || password==null || password.length()<8
                || users.findByEmailIgnoreCase(normalizedEmail).isPresent() || users.findByMobile(normalizedMobile).isPresent())
            return "redirect:/fleet?error=driverCreate";
        UserAccount driver=new UserAccount(); driver.setName(name.trim()); driver.setEmail(normalizedEmail);
        driver.setMobile(normalizedMobile); driver.setPasswordHash(passwordEncoder.encode(password));
        driver.setRole("DRIVER"); driver.setStatus("PENDING"); driver.setAvailable(false);
        driver.setMobileVerified(false); driver.setFleetOwnerId(owner.getId());
        driver.setFleetAssociationStatus("ACCEPTED"); users.save(driver);
        notifications.notifyUser(driver.getId(),"Fleet driver account created", "Your driver account was created by fleet owner "+owner.getName()+". Complete mobile verification and driver documents; admin approval is required before driving.","/partner",null,"FLEET_DRIVER_CREATED");
        return "redirect:/fleet?driverCreated";
    }

    @GetMapping("/fleet/drivers/search")
    public String searchDrivers(Authentication authentication, @RequestParam(required=false, defaultValue="") String mobile, Model model) {
        UserAccount owner=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        String query=mobile==null?"":mobile.replaceAll("\\D", "");
        List<UserAccount> matches=query.length()<3?List.of():users.findByRoleIn(List.of("DRIVER")).stream()
                .filter(d->d.isMobileVerified() && "APPROVED".equals(d.getStatus()))
                .filter(d->d.getMobile()!=null && d.getMobile().replaceAll("\\D", "").contains(query))
                .filter(d->d.getFleetOwnerId()==null || d.getFleetOwnerId().isBlank() || owner.getId().equals(d.getFleetOwnerId()))
                .filter(d->!owner.getId().equals(d.getFleetOwnerId()))
                .limit(20).toList();
        model.addAttribute("driverSearch",mobile); model.addAttribute("driverSearchResults",matches);
        return dashboard(authentication,model);
    }

    @PostMapping("/fleet/drivers/{id}/request")
    public String requestDriverAssociation(Authentication authentication, @PathVariable String id) {
        UserAccount owner=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        UserAccount driver=users.findById(id).orElse(null);
        if(!"FLEET_OWNER".equals(owner.getRole()) || driver==null || !"DRIVER".equals(driver.getRole())
                || !"APPROVED".equals(driver.getStatus()) || !driver.isMobileVerified()
                || (driver.getFleetOwnerId()!=null && !driver.getFleetOwnerId().isBlank())
                || (driver.getPendingFleetOwnerId()!=null && !driver.getPendingFleetOwnerId().isBlank())) return "redirect:/fleet?error=association";
        driver.setPendingFleetOwnerId(owner.getId()); driver.setFleetAssociationStatus("PENDING"); users.save(driver);
        notifications.notifyUser(driver.getId(),"Fleet association request",owner.getName()+" wants to add you to their fleet. Review and accept or decline in your partner dashboard.","/partner",null,"FLEET_ASSOCIATION_REQUEST");
        return "redirect:/fleet?associationRequested";
    }

    private boolean vehicleHasOverlap(String vehicleId,RideBooking current){return vehicleHasOverlap(vehicleId,current,false);}

    private boolean vehicleHasOverlap(String vehicleId,RideBooking current,boolean returnLeg){
        LocalDateTime start=returnLeg&&current.getReturnAt()!=null?current.getReturnAt():(current.getScheduledAt()!=null?current.getScheduledAt():LocalDateTime.now());
        LocalDateTime end=returnLeg?start.plusHours(4):(current.getReturnAt()!=null?current.getReturnAt():start.plusHours(4));
        for(RideBooking other:rides.findAll()){
            if(current.getId().equals(other.getId())||"CANCELLED".equals(other.getStatus()))continue;
            if("COMPLETED".equals(other.getStatus())&&(!"ROUND_TRIP".equals(other.getJourneyType())||("COMPLETED".equals(other.getReturnLegStatus())||"CANCELLED".equals(other.getReturnLegStatus())||"NOT_APPLICABLE".equals(other.getReturnLegStatus()))))continue;
            boolean linked=vehicleId.equals(other.getAssignedVehicleId())||vehicleId.equals(other.getRequestedVehicleId())||vehicleId.equals(other.getReturnAssignedVehicleId());
            if(!linked)continue;
            boolean inProgress="IN_PROGRESS".equals(other.getStatus())||"IN_PROGRESS".equals(other.getReturnLegStatus());
            LocalDateTime now=LocalDateTime.now();
            LocalDateTime oldStart=inProgress?now:(other.getScheduledAt()!=null?other.getScheduledAt():LocalDateTime.ofInstant(other.getCreatedAt()==null?Instant.now():other.getCreatedAt(),ZoneId.systemDefault()));
            LocalDateTime oldEnd=other.getReturnAt()!=null?other.getReturnAt():oldStart.plusHours(4);
            if(inProgress&&oldEnd.isBefore(now.plusHours(4)))oldEnd=now.plusHours(4);
            if(oldStart.isBefore(end.plusHours(1))&&oldEnd.isAfter(start.minusHours(1)))return true;
        }
        return false;
    }

    private boolean hasOtherActiveAssignment(String driverId,String currentRideId){return rides.findByAssignedDriverIdOrderByCreatedAtDesc(driverId).stream().anyMatch(r->!r.getId().equals(currentRideId)&&List.of("ASSIGNED","ACCEPTED","IN_PROGRESS").contains(r.getStatus()))||rides.findAll().stream().anyMatch(r->!r.getId().equals(currentRideId)&&driverId.equals(r.getReturnAssignedDriverId())&&("ACCEPTED".equals(r.getReturnLegStatus())||"IN_PROGRESS".equals(r.getReturnLegStatus())));}

    private boolean routeCovered(Vehicle v,RideBooking r){
        String from=r.getPickupCityId()+"::"+r.getPickupPinCode(),to=r.getDestinationCityId()+"::"+r.getDestinationPinCode();
        if(v.getPickupLocationKeys()==null||v.getDropLocationKeys()==null||v.getAllowedRouteKeys()==null)return false;
        if(!v.getPickupLocationKeys().contains(from)||!v.getDropLocationKeys().contains(to)||!v.getAllowedRouteKeys().contains(r.getPickupCityId()+">"+r.getDestinationCityId()))return false;
        if("ROUND_TRIP".equals(r.getJourneyType()))return v.isSupportsRoundTrip()&&v.getPickupLocationKeys().contains(to)&&v.getDropLocationKeys().contains(from)&&v.getAllowedRouteKeys().contains(r.getDestinationCityId()+">"+r.getPickupCityId());
        return v.isSupportsOneWay();
    }
    private boolean vehicleDocsApproved(Vehicle v){return documentRequirements.verifiedVehicleDocuments(v);}

    @org.springframework.web.bind.annotation.PostMapping("/fleet/rides/{id}/assign-outward")
    public String assignOutward(Authentication authentication, @org.springframework.web.bind.annotation.PathVariable String id,
                                @org.springframework.web.bind.annotation.RequestParam String driverId,
                                @org.springframework.web.bind.annotation.RequestParam String vehicleId) {
        UserAccount owner=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        RideBooking ride=rides.findById(id).orElseThrow();
        UserAccount driver=users.findById(driverId).orElse(null); Vehicle vehicle=vehicles.findById(vehicleId).orElse(null);
        if(driver==null||vehicle==null||!"DRIVER".equals(driver.getRole())||!owner.getId().equals(driver.getFleetOwnerId())||!driver.isAvailable()||hasOtherActiveAssignment(driver.getId(),ride.getId())||!driver.isMobileVerified()||!"APPROVED".equals(driver.getStatus())||(!owner.getId().equals(vehicle.getOwnerId())&& !driver.getId().equals(vehicle.getAssignedDriverId()))||(vehicle.getAssignedDriverId()!=null&&!vehicle.getAssignedDriverId().equals(driver.getId())&&users.findById(vehicle.getAssignedDriverId()).map(existingDriver->existingDriver.isAvailable()&&"APPROVED".equals(existingDriver.getStatus())&&existingDriver.isMobileVerified()).orElse(false))||!"APPROVED".equals(vehicle.getStatus())||!"ACTIVE".equals(vehicle.getOperationalStatus())||!vehicleDocsApproved(vehicle)||!routeCovered(vehicle,ride)||!ride.getCategory().equals(vehicle.getCategory())||(ride.getRequestedVehicleId()!=null&&!ride.getRequestedVehicleId().equals(vehicle.getId()))||vehicleHasOverlap(vehicle.getId(),ride)||!"REQUESTED".equals(ride.getStatus())) return "redirect:/fleet?error=assign";
        String assignmentLock=reservationLocks.tryAcquire(vehicle.getId());if(assignmentLock==null)return "redirect:/fleet?error=assign";
        if(vehicleHasOverlap(vehicle.getId(),ride)){reservationLocks.release(vehicle.getId(),assignmentLock);return "redirect:/fleet?error=assign";}
        String old=ride.getStatus();ride.setAssignedDriverId(driver.getId());ride.setAssignedDriverName(driver.getName());ride.setAssignedVehicleId(vehicle.getId());ride.setAssignedVehicleNumber(vehicle.getRegistrationNumber());ride.setPickupOtp(String.format("%06d",random.nextInt(1_000_000)));
        ride.addStatusEvent(old,"ASSIGNED",owner.getName(),"Fleet owner requested driver assignment; awaiting driver response");ride.setStatus("ASSIGNED");
        if("ROUND_TRIP".equals(ride.getJourneyType())&&ride.getReturnAssignedDriverId()==null){ride.setReturnAssignedDriverId(driver.getId());ride.setReturnAssignedDriverName(driver.getName());ride.setReturnAssignedVehicleId(vehicle.getId());ride.setReturnAssignedVehicleNumber(vehicle.getRegistrationNumber());}
        try{rides.save(ride);}finally{reservationLocks.release(vehicle.getId(),assignmentLock);}notifications.notifyUser(driver.getId(),"New ride assignment", "Fleet owner "+owner.getName()+" assigned you a ride request. Accept or decline from your driver dashboard.","/driver",ride.getId(),"DRIVER_RIDE_ASSIGNMENT");return "redirect:/fleet?assigned";
    }

    @org.springframework.web.bind.annotation.PostMapping("/fleet/rides/{id}/assign-return")
    public String assignReturn(Authentication authentication, @org.springframework.web.bind.annotation.PathVariable String id,
                               @org.springframework.web.bind.annotation.RequestParam String driverId,
                               @org.springframework.web.bind.annotation.RequestParam String vehicleId) {
        UserAccount owner=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();RideBooking ride=rides.findById(id).orElseThrow();
        UserAccount driver=users.findById(driverId).orElse(null);Vehicle vehicle=vehicles.findById(vehicleId).orElse(null);
        boolean sameOutward=driver!=null&&driver.getId().equals(ride.getAssignedDriverId());
        if(!"ROUND_TRIP".equals(ride.getJourneyType())||ride.getReturnAt()==null||!"COMPLETED".equals(ride.getStatus())||!("REQUESTED".equals(ride.getReturnLegStatus())||"ACCEPTED".equals(ride.getReturnLegStatus()))||driver==null||vehicle==null||!"DRIVER".equals(driver.getRole())||!owner.getId().equals(driver.getFleetOwnerId())||!driver.isMobileVerified()||!"APPROVED".equals(driver.getStatus())||(!sameOutward&&!driver.isAvailable())||hasOtherActiveAssignment(driver.getId(),ride.getId())||(!owner.getId().equals(vehicle.getOwnerId())&&!driver.getId().equals(vehicle.getAssignedDriverId()))||!"APPROVED".equals(vehicle.getStatus())||!"ACTIVE".equals(vehicle.getOperationalStatus())||!vehicleDocsApproved(vehicle)||!vehicle.isSupportsRoundTrip()||!ride.getCategory().equals(vehicle.getCategory())||!vehicle.getPickupLocationKeys().contains(ride.getDestinationCityId()+"::"+ride.getDestinationPinCode())||!vehicle.getDropLocationKeys().contains(ride.getPickupCityId()+"::"+ride.getPickupPinCode())||!vehicle.getAllowedRouteKeys().contains(ride.getDestinationCityId()+">"+ride.getPickupCityId())||vehicleHasOverlap(vehicle.getId(),ride,true)) return "redirect:/fleet?error=returnAssign";
        String returnLock=reservationLocks.tryAcquire(vehicle.getId());if(returnLock==null)return "redirect:/fleet?error=returnAssign";
        if(vehicleHasOverlap(vehicle.getId(),ride,true)){reservationLocks.release(vehicle.getId(),returnLock);return "redirect:/fleet?error=returnAssign";}
        String old=ride.getReturnLegStatus();ride.setReturnAssignedDriverId(driver.getId());ride.setReturnAssignedDriverName(driver.getName());ride.setReturnAssignedVehicleId(vehicle.getId());ride.setReturnAssignedVehicleNumber(vehicle.getRegistrationNumber());ride.setReturnPickupOtp(null);ride.setReturnLegStatus("ASSIGNED");ride.addStatusEvent("RETURN_"+old,"RETURN_ASSIGNED",owner.getName(),"Fleet owner requested return driver assignment; awaiting driver response");try{rides.save(ride);}finally{reservationLocks.release(vehicle.getId(),returnLock);}notifications.notifyUser(driver.getId(),"New return ride assignment","Fleet owner "+owner.getName()+" assigned you a return journey request. Accept or decline from your driver dashboard.","/driver",ride.getId(),"DRIVER_RETURN_ASSIGNMENT");return "redirect:/fleet?returnAssigned";
    }

}
