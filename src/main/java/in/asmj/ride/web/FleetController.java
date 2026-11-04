package in.asmj.ride.web;

import in.asmj.ride.model.*;
import in.asmj.ride.repo.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.*;
import java.util.stream.Collectors;
import java.security.SecureRandom;

@Controller
public class FleetController {
    private final UserAccountRepository users;
    private final VehicleRepository vehicles;
    private final RideBookingRepository rides;
    private final PaymentRecordRepository payments;
    private final SecureRandom random=new SecureRandom();

    public FleetController(UserAccountRepository users, VehicleRepository vehicles,
                           RideBookingRepository rides, PaymentRecordRepository payments) {
        this.users=users; this.vehicles=vehicles; this.rides=rides; this.payments=payments;
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
                (r.getReturnAssignedVehicleId()!=null && vehicleIds.contains(r.getReturnAssignedVehicleId())))
                .sorted(Comparator.comparing(RideBooking::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        List<PaymentRecord> fleetPayments=payments.findAll().stream().filter(p ->
                owner.getId().equals(p.getFleetOwnerId()) || owner.getId().equals(p.getReturnFleetOwnerId())).toList();
        model.addAttribute("owner",owner);
        model.addAttribute("drivers",drivers);
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
    private boolean hasOtherActiveAssignment(String driverId,String currentRideId){return rides.findByAssignedDriverIdOrderByCreatedAtDesc(driverId).stream().anyMatch(r->!r.getId().equals(currentRideId)&&List.of("ACCEPTED","IN_PROGRESS").contains(r.getStatus()))||rides.findAll().stream().anyMatch(r->!r.getId().equals(currentRideId)&&driverId.equals(r.getReturnAssignedDriverId())&&("ACCEPTED".equals(r.getReturnLegStatus())||"IN_PROGRESS".equals(r.getReturnLegStatus())));}

    @org.springframework.web.bind.annotation.PostMapping("/fleet/rides/{id}/assign-outward")
    public String assignOutward(Authentication authentication, @org.springframework.web.bind.annotation.PathVariable String id,
                                @org.springframework.web.bind.annotation.RequestParam String driverId,
                                @org.springframework.web.bind.annotation.RequestParam String vehicleId) {
        UserAccount owner=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        RideBooking ride=rides.findById(id).orElseThrow();
        UserAccount driver=users.findById(driverId).orElse(null); Vehicle vehicle=vehicles.findById(vehicleId).orElse(null);
        if(driver==null||vehicle==null||!"DRIVER".equals(driver.getRole())||!owner.getId().equals(driver.getFleetOwnerId())||!driver.isAvailable()||hasOtherActiveAssignment(driver.getId(),ride.getId())||!driver.isMobileVerified()||!"APPROVED".equals(driver.getStatus())||!owner.getId().equals(vehicle.getOwnerId())&& !driver.getId().equals(vehicle.getAssignedDriverId())||!"APPROVED".equals(vehicle.getStatus())||!ride.getCategory().equals(vehicle.getCategory())||!"REQUESTED".equals(ride.getStatus())) return "redirect:/fleet?error=assign";
        String old=ride.getStatus();ride.setAssignedDriverId(driver.getId());ride.setAssignedDriverName(driver.getName());ride.setAssignedVehicleId(vehicle.getId());ride.setAssignedVehicleNumber(vehicle.getRegistrationNumber());ride.setPickupOtp(String.format("%06d",random.nextInt(1_000_000)));
        ride.addStatusEvent(old,"ACCEPTED",owner.getName(),"Fleet owner assigned outward driver and vehicle");ride.setStatus("ACCEPTED");
        if("ROUND_TRIP".equals(ride.getJourneyType())&&ride.getReturnAssignedDriverId()==null){ride.setReturnAssignedDriverId(driver.getId());ride.setReturnAssignedDriverName(driver.getName());ride.setReturnAssignedVehicleId(vehicle.getId());ride.setReturnAssignedVehicleNumber(vehicle.getRegistrationNumber());}
        rides.save(ride);return "redirect:/fleet?assigned";
    }

    @org.springframework.web.bind.annotation.PostMapping("/fleet/rides/{id}/assign-return")
    public String assignReturn(Authentication authentication, @org.springframework.web.bind.annotation.PathVariable String id,
                               @org.springframework.web.bind.annotation.RequestParam String driverId,
                               @org.springframework.web.bind.annotation.RequestParam String vehicleId) {
        UserAccount owner=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();RideBooking ride=rides.findById(id).orElseThrow();
        UserAccount driver=users.findById(driverId).orElse(null);Vehicle vehicle=vehicles.findById(vehicleId).orElse(null);
        boolean sameOutward=driver!=null&&driver.getId().equals(ride.getAssignedDriverId());
        if(!"ROUND_TRIP".equals(ride.getJourneyType())||!"COMPLETED".equals(ride.getStatus())||!("REQUESTED".equals(ride.getReturnLegStatus())||"ACCEPTED".equals(ride.getReturnLegStatus()))||driver==null||vehicle==null||!"DRIVER".equals(driver.getRole())||!owner.getId().equals(driver.getFleetOwnerId())||!driver.isMobileVerified()||!"APPROVED".equals(driver.getStatus())||(!sameOutward&&!driver.isAvailable())||hasOtherActiveAssignment(driver.getId(),ride.getId())||(!owner.getId().equals(vehicle.getOwnerId())&&!driver.getId().equals(vehicle.getAssignedDriverId()))||!"APPROVED".equals(vehicle.getStatus())||!ride.getCategory().equals(vehicle.getCategory())) return "redirect:/fleet?error=returnAssign";
        String old=ride.getReturnLegStatus();ride.setReturnAssignedDriverId(driver.getId());ride.setReturnAssignedDriverName(driver.getName());ride.setReturnAssignedVehicleId(vehicle.getId());ride.setReturnAssignedVehicleNumber(vehicle.getRegistrationNumber());ride.setReturnPickupOtp(String.format("%06d",random.nextInt(1_000_000)));ride.setReturnLegStatus("ACCEPTED");ride.addStatusEvent("RETURN_"+old,"RETURN_ACCEPTED",owner.getName(),"Fleet owner assigned return driver and vehicle");rides.save(ride);return "redirect:/fleet?returnAssigned";
    }

}
