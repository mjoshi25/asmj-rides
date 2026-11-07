package in.asmj.ride.web;

import in.asmj.ride.model.*;
import in.asmj.ride.repo.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.*;
import java.time.*;
import in.asmj.ride.service.RideNotificationService;
import in.asmj.ride.service.VehicleReservationLockService;
import in.asmj.ride.service.DriverAvailabilityService;
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
    private final DriverAvailabilityService driverAvailability;
    private final SecureRandom random=new SecureRandom();

    public FleetController(UserAccountRepository users, VehicleRepository vehicles,
                           RideBookingRepository rides, PaymentRecordRepository payments,
                           RideNotificationService notifications, VehicleReservationLockService reservationLocks, DriverAvailabilityService driverAvailability) {
        this.users=users; this.vehicles=vehicles; this.rides=rides; this.payments=payments; this.notifications=notifications; this.reservationLocks=reservationLocks; this.driverAvailability=driverAvailability;
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
    private boolean vehicleHasOverlap(String vehicleId,RideBooking current){return vehicleHasOverlap(vehicleId,current,false);}
    private boolean vehicleHasOverlap(String vehicleId,RideBooking current,boolean returnLeg){
        LocalDateTime start=returnLeg&&current.getReturnAt()!=null?current.getReturnAt():(current.getScheduledAt()!=null?current.getScheduledAt():LocalDateTime.now());
        LocalDateTime end=returnLeg?start.plusHours(4):(current.getReturnAt()!=null?current.getReturnAt().plusHours(4):start.plusHours(4));
        for(RideBooking other:rides.findAll()){
            if(current.getId().equals(other.getId())||"CANCELLED".equals(other.getStatus()))continue;
            boolean outwardLinked=vehicleId.equals(other.getAssignedVehicleId())||vehicleId.equals(other.getRequestedVehicleId());
            boolean returnLinked=vehicleId.equals(other.getReturnAssignedVehicleId());
            if(!outwardLinked&&!returnLinked)continue;
            LocalDateTime now=LocalDateTime.now();
            LocalDateTime bookingStart=other.getScheduledAt()!=null?other.getScheduledAt():LocalDateTime.ofInstant(other.getCreatedAt()==null?Instant.now():other.getCreatedAt(),ZoneId.systemDefault());
            if(outwardLinked){
                LocalDateTime busyStart=bookingStart,busyEnd=busyStart.plusHours(4);
                boolean activeOutward=List.of("ACCEPTED","ASSIGNED","IN_PROGRESS").contains(other.getStatus());
                if(activeOutward&&("IN_PROGRESS".equals(other.getStatus())||other.getScheduledAt()==null||!busyEnd.isAfter(now))){busyStart=now;busyEnd=now.plusHours(4);}
                boolean returnStillReserved="ROUND_TRIP".equals(other.getJourneyType())&&other.getReturnAt()!=null&&!List.of("COMPLETED","CANCELLED","NOT_APPLICABLE").contains(other.getReturnLegStatus())&&(other.getReturnAssignedVehicleId()==null||vehicleId.equals(other.getReturnAssignedVehicleId()));
                if(returnStillReserved){LocalDateTime returnEnd=other.getReturnAt().plusHours(4);if(returnEnd.isBefore(now.plusHours(4)))returnEnd=now.plusHours(4);if(returnEnd.isAfter(busyEnd))busyEnd=returnEnd;}
                if(busyStart.isBefore(end.plusHours(1))&&busyEnd.isAfter(start.minusHours(1)))return true;
            }
            if(returnLinked&&"ROUND_TRIP".equals(other.getJourneyType())&&other.getReturnAt()!=null&&!List.of("COMPLETED","CANCELLED","NOT_APPLICABLE").contains(other.getReturnLegStatus())){
                LocalDateTime busyStart=other.getReturnAt(),busyEnd=busyStart.plusHours(4);
                if("IN_PROGRESS".equals(other.getReturnLegStatus())||!busyEnd.isAfter(now)){busyStart=now;busyEnd=now.plusHours(4);}
                if(busyStart.isBefore(end.plusHours(1))&&busyEnd.isAfter(start.minusHours(1)))return true;
            }
        }
        return false;
    }


    private boolean routeCovered(Vehicle v,RideBooking r){
        String from=r.getPickupCityId()+"::"+r.getPickupPinCode(),to=r.getDestinationCityId()+"::"+r.getDestinationPinCode();
        if(v.getPickupLocationKeys()==null||v.getDropLocationKeys()==null||v.getAllowedRouteKeys()==null)return false;
        if(!v.getPickupLocationKeys().contains(from)||!v.getDropLocationKeys().contains(to)||!v.getAllowedRouteKeys().contains(r.getPickupCityId()+">"+r.getDestinationCityId()))return false;
        if("ROUND_TRIP".equals(r.getJourneyType()))return v.isSupportsRoundTrip()&&v.getPickupLocationKeys().contains(to)&&v.getDropLocationKeys().contains(from)&&v.getAllowedRouteKeys().contains(r.getDestinationCityId()+">"+r.getPickupCityId());
        return v.isSupportsOneWay();
    }
    private boolean vehicleDocsApproved(Vehicle v){return v.getPhotos()!=null&&v.getPhotos().stream().anyMatch(p->"VERIFIED".equals(p.getVerificationStatus()))&&v.getDocuments()!=null&&List.of("RC","INSURANCE").stream().allMatch(t->v.getDocuments().stream().anyMatch(d->t.equals(d.getCategory())&&"VERIFIED".equals(d.getVerificationStatus())));}

    @org.springframework.web.bind.annotation.PostMapping("/fleet/rides/{id}/assign-outward")
    public String assignOutward(Authentication authentication, @org.springframework.web.bind.annotation.PathVariable String id,
                                @org.springframework.web.bind.annotation.RequestParam String driverId,
                                @org.springframework.web.bind.annotation.RequestParam String vehicleId) {
        UserAccount owner=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        if(!"APPROVED".equals(owner.getStatus())||!owner.isMobileVerified())return "redirect:/fleet?error=approval";
        String rideLock=reservationLocks.tryAcquireRide(id);if(rideLock==null)return "redirect:/fleet?error=assign";
        String vehicleLock=null,driverLock=null,lockedVehicleId=null;RideBooking ride=null;UserAccount driver=users.findById(driverId).orElse(null);Vehicle vehicle=null;
        try{
            ride=rides.findById(id).orElseThrow();
            if(driver==null||!"DRIVER".equals(driver.getRole())||!owner.getId().equals(driver.getFleetOwnerId())||!"APPROVED".equals(driver.getStatus())||!driver.isMobileVerified()||List.of("IN_PROGRESS","COMPLETED","CANCELLED").contains(ride.getStatus()))return "redirect:/fleet?error=assign";
            vehicle=vehicles.findById(vehicleId).orElse(null);
            if(vehicle==null||!owner.getId().equals(vehicle.getOwnerId())||!"APPROVED".equals(vehicle.getStatus())||!"ACTIVE".equals(vehicle.getOperationalStatus())||!vehicleDocsApproved(vehicle)||!routeCovered(vehicle,ride)||!ride.getCategory().equals(vehicle.getCategory())||(ride.getRequestedVehicleId()!=null&&!ride.getRequestedVehicleId().equals(vehicle.getId()))||!"REQUESTED".equals(ride.getStatus()))return "redirect:/fleet?error=assign";
            LocalDateTime start=ride.getScheduledAt()!=null?ride.getScheduledAt():LocalDateTime.now(),end=ride.getReturnAt()!=null?ride.getReturnAt().plusHours(4):start.plusHours(4);
            if(!driverAvailability.isAvailable(driver,start,end,ride.getId()))return "redirect:/fleet?error=assign";
            if(vehicle.getAssignedDriverId()!=null&&!vehicle.getAssignedDriverId().equals(driver.getId())){UserAccount existing=users.findById(vehicle.getAssignedDriverId()).orElse(null);if(existing!=null&&driverAvailability.isAvailable(existing,start,end,ride.getId()))return "redirect:/fleet?error=assign";}
            lockedVehicleId=vehicle.getId();vehicleLock=reservationLocks.tryAcquire(lockedVehicleId);if(vehicleLock==null)return "redirect:/fleet?error=assign";
            driverLock=reservationLocks.tryAcquireDriver(driver.getId());if(driverLock==null)return "redirect:/fleet?error=assign";
            driver=users.findById(driverId).orElseThrow();ride=rides.findById(id).orElseThrow();vehicle=vehicles.findById(lockedVehicleId).orElse(null);
            if(vehicle==null||!owner.getId().equals(vehicle.getOwnerId())||!owner.getId().equals(driver.getFleetOwnerId())||!"APPROVED".equals(driver.getStatus())||!driver.isMobileVerified()||!"REQUESTED".equals(ride.getStatus())||!driverAvailability.isAvailable(driver,start,end,ride.getId())||(vehicle.getAssignedDriverId()!=null&&!vehicle.getAssignedDriverId().equals(driver.getId())&&users.findById(vehicle.getAssignedDriverId()).map(existing->driverAvailability.isAvailable(existing,start,end,id)).orElse(false))||!"APPROVED".equals(vehicle.getStatus())||!"ACTIVE".equals(vehicle.getOperationalStatus())||!vehicleDocsApproved(vehicle)||!routeCovered(vehicle,ride)||!ride.getCategory().equals(vehicle.getCategory())||(ride.getRequestedVehicleId()!=null&&!ride.getRequestedVehicleId().equals(vehicle.getId()))||vehicleHasOverlap(vehicle.getId(),ride))return "redirect:/fleet?error=assign";
            String old=ride.getStatus();boolean preferredReturn="ROUND_TRIP".equals(ride.getJourneyType())&&"REQUESTED".equals(ride.getReturnLegStatus())&&(ride.getReturnAssignedDriverId()==null||(Objects.equals(ride.getReturnAssignedDriverId(),ride.getAssignedDriverId())&&Objects.equals(ride.getReturnAssignedVehicleId(),ride.getAssignedVehicleId())));
            ride.setAssignedDriverId(driver.getId());ride.setAssignedDriverName(driver.getName());ride.setAssignedVehicleId(vehicle.getId());ride.setAssignedVehicleNumber(vehicle.getRegistrationNumber());ride.setPickupOtp(String.format("%06d",random.nextInt(1_000_000)));
            if(preferredReturn){ride.setReturnAssignedDriverId(driver.getId());ride.setReturnAssignedDriverName(driver.getName());ride.setReturnAssignedVehicleId(vehicle.getId());ride.setReturnAssignedVehicleNumber(vehicle.getRegistrationNumber());}
            ride.addStatusEvent(old,"ACCEPTED",owner.getName(),"Fleet owner assigned outward driver and vehicle");ride.setStatus("ACCEPTED");rides.save(ride);
        }finally{if(driverLock!=null&&driver!=null)reservationLocks.releaseDriver(driver.getId(),driverLock);if(vehicleLock!=null&&lockedVehicleId!=null)reservationLocks.release(lockedVehicleId,vehicleLock);reservationLocks.releaseRide(id,rideLock);}
        notifications.notifyUser(ride.getCustomerId(),"Driver assigned",driver.getName()+" has been assigned to your ride. Vehicle: "+ride.getAssignedVehicleNumber()+".","/customer",ride.getId(),"RIDE_ASSIGNED");return "redirect:/fleet?assigned";
    }

    @org.springframework.web.bind.annotation.PostMapping("/fleet/rides/{id}/assign-return")
    public String assignReturn(Authentication authentication, @org.springframework.web.bind.annotation.PathVariable String id,
                               @org.springframework.web.bind.annotation.RequestParam String driverId,
                               @org.springframework.web.bind.annotation.RequestParam String vehicleId) {
        UserAccount owner=users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        if(!"APPROVED".equals(owner.getStatus())||!owner.isMobileVerified())return "redirect:/fleet?error=approval";
        String rideLock=reservationLocks.tryAcquireRide(id);if(rideLock==null)return "redirect:/fleet?error=returnAssign";
        String vehicleLock=null,driverLock=null,lockedVehicleId=null;RideBooking ride=null;UserAccount driver=users.findById(driverId).orElse(null);Vehicle vehicle=null;
        try{
            ride=rides.findById(id).orElseThrow();boolean sameOutward=driver!=null&&driver.getId().equals(ride.getAssignedDriverId());
            if(!"ROUND_TRIP".equals(ride.getJourneyType())||ride.getReturnAt()==null||!"COMPLETED".equals(ride.getStatus())||!("REQUESTED".equals(ride.getReturnLegStatus())||"ACCEPTED".equals(ride.getReturnLegStatus()))||driver==null||!"DRIVER".equals(driver.getRole())||!owner.getId().equals(driver.getFleetOwnerId())||!driver.isMobileVerified()||!"APPROVED".equals(driver.getStatus()))return "redirect:/fleet?error=returnAssign";
            vehicle=vehicles.findById(vehicleId).orElse(null);
            if(vehicle==null||!owner.getId().equals(vehicle.getOwnerId())||!"APPROVED".equals(vehicle.getStatus())||!"ACTIVE".equals(vehicle.getOperationalStatus())||!vehicleDocsApproved(vehicle)||!vehicle.isSupportsRoundTrip()||!ride.getCategory().equals(vehicle.getCategory())||!vehicle.getPickupLocationKeys().contains(ride.getDestinationCityId()+"::"+ride.getDestinationPinCode())||!vehicle.getDropLocationKeys().contains(ride.getPickupCityId()+"::"+ride.getPickupPinCode())||!vehicle.getAllowedRouteKeys().contains(ride.getDestinationCityId()+">"+ride.getPickupCityId()))return "redirect:/fleet?error=returnAssign";
            LocalDateTime start=ride.getReturnAt(),end=start.plusHours(4);
            if(sameOutward?driverAvailability.hasConflict(driver,start,end,ride.getId()):!driverAvailability.isAvailable(driver,start,end,ride.getId()))return "redirect:/fleet?error=returnAssign";
            lockedVehicleId=vehicle.getId();vehicleLock=reservationLocks.tryAcquire(lockedVehicleId);if(vehicleLock==null)return "redirect:/fleet?error=returnAssign";
            driverLock=reservationLocks.tryAcquireDriver(driver.getId());if(driverLock==null)return "redirect:/fleet?error=returnAssign";
            driver=users.findById(driverId).orElseThrow();ride=rides.findById(id).orElseThrow();vehicle=vehicles.findById(lockedVehicleId).orElse(null);sameOutward=driver.getId().equals(ride.getAssignedDriverId());
            if(vehicle==null||!owner.getId().equals(vehicle.getOwnerId())||!owner.getId().equals(driver.getFleetOwnerId())||!"COMPLETED".equals(ride.getStatus())||!"ROUND_TRIP".equals(ride.getJourneyType())||!"APPROVED".equals(driver.getStatus())||!driver.isMobileVerified()||(sameOutward?driverAvailability.hasConflict(driver,start,end,ride.getId()):!driverAvailability.isAvailable(driver,start,end,ride.getId()))||!"APPROVED".equals(vehicle.getStatus())||!"ACTIVE".equals(vehicle.getOperationalStatus())||!vehicleDocsApproved(vehicle)||!vehicle.isSupportsRoundTrip()||!ride.getCategory().equals(vehicle.getCategory())||!vehicle.getPickupLocationKeys().contains(ride.getDestinationCityId()+"::"+ride.getDestinationPinCode())||!vehicle.getDropLocationKeys().contains(ride.getPickupCityId()+"::"+ride.getPickupPinCode())||!vehicle.getAllowedRouteKeys().contains(ride.getDestinationCityId()+">"+ride.getPickupCityId())||vehicleHasOverlap(vehicle.getId(),ride,true))return "redirect:/fleet?error=returnAssign";
            String old=ride.getReturnLegStatus();ride.setReturnAssignedDriverId(driver.getId());ride.setReturnAssignedDriverName(driver.getName());ride.setReturnAssignedVehicleId(vehicle.getId());ride.setReturnAssignedVehicleNumber(vehicle.getRegistrationNumber());ride.setReturnPickupOtp(String.format("%06d",random.nextInt(1_000_000)));ride.setReturnLegStatus("ACCEPTED");ride.addStatusEvent("RETURN_"+old,"RETURN_ACCEPTED",owner.getName(),"Fleet owner assigned return driver and vehicle");rides.save(ride);
        }finally{if(driverLock!=null&&driver!=null)reservationLocks.releaseDriver(driver.getId(),driverLock);if(vehicleLock!=null&&lockedVehicleId!=null)reservationLocks.release(lockedVehicleId,vehicleLock);reservationLocks.releaseRide(id,rideLock);}
        notifications.notifyUser(ride.getCustomerId(),"Return driver assigned",driver.getName()+" has been assigned to your return journey. Vehicle: "+ride.getReturnAssignedVehicleNumber()+".","/customer",ride.getId(),"RETURN_ASSIGNED");return "redirect:/fleet?returnAssigned";
    }


}
