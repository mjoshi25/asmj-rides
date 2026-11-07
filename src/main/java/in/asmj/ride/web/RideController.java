package in.asmj.ride.web;
import in.asmj.ride.model.*;import in.asmj.ride.repo.*;import in.asmj.ride.service.FareCalculator;import in.asmj.ride.service.RideNotificationService;import in.asmj.ride.service.VehicleReservationLockService;import in.asmj.ride.service.DriverAvailabilityService;
import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import org.springframework.http.ResponseEntity;
import java.time.*;import java.util.*;import java.security.SecureRandom;
@Controller
public class RideController {
 private final UserAccountRepository users; private final RideBookingRepository rides; private final VehicleRepository vehicles; private final FareRuleRepository fareRules; private final FareCalculator fareCalculator; private final ServiceCityRepository cities; private final PaymentRecordRepository payments; private final RouteDistanceRepository routeDistances; private final RideNotificationService notifications; private final VehicleReservationLockService reservationLocks; private final DriverAvailabilityService driverAvailability; private final SecureRandom random=new SecureRandom();
 public RideController(UserAccountRepository u,RideBookingRepository r,VehicleRepository v,FareRuleRepository f,FareCalculator c,ServiceCityRepository cities,PaymentRecordRepository payments,RouteDistanceRepository routeDistances,RideNotificationService notifications,VehicleReservationLockService reservationLocks,DriverAvailabilityService driverAvailability){users=u;rides=r;vehicles=v;fareRules=f;fareCalculator=c;this.cities=cities;this.payments=payments;this.routeDistances=routeDistances;this.notifications=notifications;this.reservationLocks=reservationLocks;this.driverAvailability=driverAvailability;}
 private UserAccount me(Authentication a){return users.findByEmailIgnoreCase(a.getName()).orElseThrow();}
 private FareRule fare(){return fareRules.findById("default").orElseGet(()->fareRules.save(new FareRule()));}
 private UserAccount availableDriverFor(Vehicle v,UserAccount owner,LocalDateTime start,LocalDateTime end,String excludeRideId){
  if(owner==null)return null;
  if(v.getAssignedDriverId()!=null){UserAccount assigned=users.findById(v.getAssignedDriverId()).orElse(null);if(assigned!=null&&(!"FLEET_OWNER".equals(owner.getRole())||owner.getId().equals(assigned.getFleetOwnerId()))&&driverAvailability.isAvailable(assigned,start,end,excludeRideId))return assigned;}
  if("DRIVER".equals(owner.getRole())&&driverAvailability.isAvailable(owner,start,end,excludeRideId))return owner;
  if("FLEET_OWNER".equals(owner.getRole()))return users.findByFleetOwnerIdAndRoleIn(owner.getId(),List.of("DRIVER")).stream().filter(d->driverAvailability.isAvailable(d,start,end,excludeRideId)).findFirst().orElse(null);
  return null;
 }
 private UserAccount availableDriverFor(Vehicle v,UserAccount owner,LocalDateTime start,LocalDateTime end){return availableDriverFor(v,owner,start,end,null);}


 @GetMapping("/customer") String customer(Authentication a,Model m){UserAccount u=me(a);m.addAttribute("name",u.getName());m.addAttribute("user",u);m.addAttribute("rides",rides.findByCustomerIdOrderByCreatedAtDesc(u.getId()));m.addAttribute("fareRule",fare());m.addAttribute("cities",cities.findByActiveTrueOrderByCityAsc());return "customer";}

 private RouteDistance findRoute(String fromCity,String fromPin,String toCity,String toPin){
  List<RouteDistance> all=routeDistances.findAll().stream().filter(RouteDistance::isActive).toList();
  RouteDistance exact=all.stream().filter(x->Objects.equals(x.getSourceCityId(),fromCity)&&Objects.equals(x.getDestinationCityId(),toCity)&&Objects.equals(x.getSourcePinCode(),fromPin)&&Objects.equals(x.getDestinationPinCode(),toPin)).findFirst().orElse(null);
  if(exact!=null)return exact;
  exact=all.stream().filter(x->Objects.equals(x.getSourceCityId(),toCity)&&Objects.equals(x.getDestinationCityId(),fromCity)&&Objects.equals(x.getSourcePinCode(),toPin)&&Objects.equals(x.getDestinationPinCode(),fromPin)).findFirst().orElse(null);
  if(exact!=null)return exact;
  RouteDistance city=all.stream().filter(x->Objects.equals(x.getSourceCityId(),fromCity)&&Objects.equals(x.getDestinationCityId(),toCity)&&(x.getSourcePinCode()==null||x.getSourcePinCode().isBlank())&&(x.getDestinationPinCode()==null||x.getDestinationPinCode().isBlank())).findFirst().orElse(null);
  if(city!=null)return city;
  return all.stream().filter(x->Objects.equals(x.getSourceCityId(),toCity)&&Objects.equals(x.getDestinationCityId(),fromCity)&&(x.getSourcePinCode()==null||x.getSourcePinCode().isBlank())&&(x.getDestinationPinCode()==null||x.getDestinationPinCode().isBlank())).findFirst().orElse(null);
 }
 @GetMapping("/rides/estimate") @ResponseBody ResponseEntity<Map<String,Object>> estimate(@RequestParam String pickupCityId,@RequestParam String pickupPinCode,@RequestParam String destinationCityId,@RequestParam String destinationPinCode,@RequestParam String category,@RequestParam(defaultValue="ONE_WAY") String journeyType){
  Map<String,Object> data=new LinkedHashMap<>();
  if(!List.of("CAR","AUTO","BIKE","OTHER").contains(category)||!List.of("ONE_WAY","ROUND_TRIP").contains(journeyType))return ResponseEntity.badRequest().body(Map.of("configured",false,"message","Invalid journey details."));
  ServiceCity from=cities.findById(pickupCityId).filter(ServiceCity::isActive).orElse(null),to=cities.findById(destinationCityId).filter(ServiceCity::isActive).orElse(null);
  if(from==null||to==null||!from.getPinCodes().contains(pickupPinCode)||!to.getPinCodes().contains(destinationPinCode))return ResponseEntity.badRequest().body(Map.of("configured",false,"message","Select valid service cities and PIN codes."));
  RouteDistance route=findRoute(pickupCityId,pickupPinCode,destinationCityId,destinationPinCode);
  if(route==null){data.put("configured",false);data.put("message","An estimated road distance is not configured for this route yet. Enter an approximate distance or contact ASMJ Ride support.");return ResponseEntity.ok(data);}
  double multiplier="ROUND_TRIP".equals(journeyType)?2:1;
  double oneWay=route.getDistanceKm(),total=oneWay*multiplier;
  data.put("configured",true);data.put("oneWayDistanceKm",oneWay);data.put("distanceKm",total);data.put("fare",fareCalculator.estimate(fare(),category,oneWay)*multiplier);data.put("roundTrip",multiplier==2);data.put("sourceNote",route.getSourceNote());
  return ResponseEntity.ok(data);
 }
 private boolean vehicleAvailable(Vehicle v,LocalDateTime start,LocalDateTime end){return vehicleAvailable(v,start,end,null);}
 private boolean vehicleAvailable(Vehicle v,LocalDateTime start,LocalDateTime end,String excludeRideId){
  for(RideBooking existing:rides.findAll()){
   if(excludeRideId!=null&&excludeRideId.equals(existing.getId()))continue;
   if("CANCELLED".equals(existing.getStatus()))continue;
   boolean outwardLinked=v.getId().equals(existing.getAssignedVehicleId())||v.getId().equals(existing.getRequestedVehicleId());
   boolean returnLinked=v.getId().equals(existing.getReturnAssignedVehicleId());
   if(!outwardLinked&&!returnLinked)continue;
   LocalDateTime now=LocalDateTime.now();
   LocalDateTime bookingStart=existing.getScheduledAt()!=null?existing.getScheduledAt():LocalDateTime.ofInstant(existing.getCreatedAt()==null?Instant.now():existing.getCreatedAt(),ZoneId.systemDefault());
   if(outwardLinked){
    LocalDateTime busyStart=bookingStart,busyEnd=busyStart.plusHours(4);
    boolean activeOutward=List.of("ACCEPTED","ASSIGNED","IN_PROGRESS").contains(existing.getStatus());
    if(activeOutward&&("IN_PROGRESS".equals(existing.getStatus())||existing.getScheduledAt()==null||!busyEnd.isAfter(now))){busyStart=now;busyEnd=now.plusHours(4);}
    boolean returnStillReserved="ROUND_TRIP".equals(existing.getJourneyType())&&existing.getReturnAt()!=null
      &&!List.of("COMPLETED","CANCELLED","NOT_APPLICABLE").contains(existing.getReturnLegStatus())
      &&(existing.getReturnAssignedVehicleId()==null||v.getId().equals(existing.getReturnAssignedVehicleId()));
    if(returnStillReserved){LocalDateTime returnEnd=existing.getReturnAt().plusHours(4);if(returnEnd.isBefore(now.plusHours(4)))returnEnd=now.plusHours(4);if(returnEnd.isAfter(busyEnd))busyEnd=returnEnd;}
    if(busyStart.isBefore(end.plusHours(1))&&busyEnd.isAfter(start.minusHours(1)))return false;
   }
   if(returnLinked&&"ROUND_TRIP".equals(existing.getJourneyType())&&existing.getReturnAt()!=null&&!List.of("COMPLETED","CANCELLED","NOT_APPLICABLE").contains(existing.getReturnLegStatus())){
    LocalDateTime busyStart=existing.getReturnAt(),busyEnd=busyStart.plusHours(4);
    if("IN_PROGRESS".equals(existing.getReturnLegStatus())||!busyEnd.isAfter(now)){busyStart=now;busyEnd=now.plusHours(4);}
    if(busyStart.isBefore(end.plusHours(1))&&busyEnd.isAfter(start.minusHours(1)))return false;
   }
  }
  return true;
 }

 private String locationKey(String cityId,String pin){return cityId+"::"+pin;}
 private boolean coversRoute(Vehicle v,String pickupCityId,String pickupPin,String destinationCityId,String destinationPin,String journeyType){
  if("ROUND_TRIP".equals(journeyType)?!v.isSupportsRoundTrip():!v.isSupportsOneWay())return false;
  if(v.getPickupLocationKeys()==null||v.getPickupLocationKeys().isEmpty()||v.getDropLocationKeys()==null||v.getDropLocationKeys().isEmpty())return false;
  if(!v.getPickupLocationKeys().contains(locationKey(pickupCityId,pickupPin))||!v.getDropLocationKeys().contains(locationKey(destinationCityId,destinationPin)))return false;
  List<String> routes=v.getAllowedRouteKeys();if(routes==null||routes.isEmpty()||!routes.contains(pickupCityId+">"+destinationCityId))return false;
  if("ROUND_TRIP".equals(journeyType))return v.getPickupLocationKeys().contains(locationKey(destinationCityId,destinationPin))&&v.getDropLocationKeys().contains(locationKey(pickupCityId,pickupPin))&&routes.contains(destinationCityId+">"+pickupCityId);
  return true;
 }
 private boolean documentsApproved(Vehicle v){
  return v.getPhotos()!=null&&v.getPhotos().stream().anyMatch(p->"VERIFIED".equals(p.getVerificationStatus()))
   &&v.getDocuments()!=null&&List.of("RC","INSURANCE").stream().allMatch(type->v.getDocuments().stream().anyMatch(d->type.equals(d.getCategory())&&"VERIFIED".equals(d.getVerificationStatus())));
 }
 private boolean coversReturnRoute(Vehicle v,RideBooking r){
  return v.isSupportsRoundTrip()&&v.getPickupLocationKeys()!=null&&v.getDropLocationKeys()!=null
   &&v.getPickupLocationKeys().contains(locationKey(r.getDestinationCityId(),r.getDestinationPinCode()))
   &&v.getDropLocationKeys().contains(locationKey(r.getPickupCityId(),r.getPickupPinCode()))
   &&v.getAllowedRouteKeys()!=null&&v.getAllowedRouteKeys().contains(r.getDestinationCityId()+">"+r.getPickupCityId());
 }
 @GetMapping("/rides/available-vehicles") @ResponseBody ResponseEntity<List<Map<String,Object>>> availableVehicles(Authentication a,@RequestParam String category,@RequestParam String pickupCityId,@RequestParam String pickupPinCode,@RequestParam String destinationCityId,@RequestParam String destinationPinCode,@RequestParam(defaultValue="ONE_WAY") String journeyType,@RequestParam(defaultValue="IMMEDIATE") String tripType,@RequestParam(required=false) String scheduledAt,@RequestParam(required=false) String returnAt){
  me(a);
  ServiceCity pickupCity=cities.findById(pickupCityId).filter(ServiceCity::isActive).orElse(null),dropCity=cities.findById(destinationCityId).filter(ServiceCity::isActive).orElse(null);
  if(pickupCity==null||dropCity==null||!pickupCity.getPinCodes().contains(pickupPinCode)||!dropCity.getPinCodes().contains(destinationPinCode)||!List.of("CAR","AUTO","BIKE","OTHER").contains(category)||!List.of("IMMEDIATE","SCHEDULED").contains(tripType)||!List.of("ONE_WAY","ROUND_TRIP").contains(journeyType))return ResponseEntity.badRequest().body(List.of());
  LocalDateTime start=LocalDateTime.now(),end=start.plusHours(4);
  try{if("SCHEDULED".equals(tripType)){start=LocalDateTime.parse(scheduledAt);if(start.isBefore(LocalDateTime.now().plusMinutes(30))||start.isAfter(LocalDateTime.now().plusDays(30)))return ResponseEntity.badRequest().body(List.of());end=start.plusHours(4);}if("ROUND_TRIP".equals(journeyType)){LocalDateTime returnTime=LocalDateTime.parse(returnAt);if(!returnTime.isAfter(start)||returnTime.isAfter(LocalDateTime.now().plusDays(30)))return ResponseEntity.badRequest().body(List.of());end=returnTime.plusHours(4);}}catch(Exception ex){return ResponseEntity.badRequest().body(List.of());}
  final LocalDateTime journeyStart=start,journeyEnd=end;
  List<Map<String,Object>> result=new ArrayList<>();
  for(Vehicle v:vehicles.findByStatus("APPROVED")){
   if(!category.equals(v.getCategory())||!"ACTIVE".equals(v.getOperationalStatus())||!documentsApproved(v)||!coversRoute(v,pickupCityId,pickupPinCode,destinationCityId,destinationPinCode,journeyType)||!vehicleAvailable(v,journeyStart,journeyEnd))continue;
   UserAccount owner=users.findById(v.getOwnerId()).orElse(null);if(owner==null||!"APPROVED".equals(owner.getStatus())||!owner.isMobileVerified()||!List.of("DRIVER","FLEET_OWNER").contains(owner.getRole()))continue;
   UserAccount driver=availableDriverFor(v,owner,journeyStart,journeyEnd);
   if(driver==null)continue;
   Map<String,Object> item=new LinkedHashMap<>();item.put("id",v.getId());item.put("category",v.getCategory());item.put("registrationNumber",v.getRegistrationNumber());item.put("model",v.getModel());item.put("baseCity",v.getBaseCity()==null?"Configured service area":v.getBaseCity());item.put("driverAvailable",true);item.put("driverAssignmentPending","FLEET_OWNER".equals(owner.getRole())&&!driver.getId().equals(v.getAssignedDriverId()));item.put("fleetVehicle","FLEET_OWNER".equals(owner.getRole()));
   UploadedAsset displayPhoto=v.getPhotos().stream().filter(p->"VERIFIED".equals(p.getVerificationStatus())).findFirst().orElse(null);if(displayPhoto!=null){String publicId=displayPhoto.getPublicId();item.put("photoUrl",publicId!=null&&publicId.startsWith("demo:")?publicId.substring(5):"/files/vehicle-photo/"+v.getId()+"/"+displayPhoto.getId());}
   result.add(item);
  }
  return ResponseEntity.ok(result);
 }
 @PostMapping("/rides/book") String book(Authentication a,@RequestParam String pickup,@RequestParam String destination,@RequestParam String pickupCityId,@RequestParam String pickupPinCode,@RequestParam String destinationCityId,@RequestParam String destinationPinCode,@RequestParam(defaultValue="ONE_WAY") String journeyType,@RequestParam(required=false) String returnAt,@RequestParam String category,@RequestParam String tripType,@RequestParam(required=false) String scheduledAt,@RequestParam String paymentMethod,@RequestParam(required=false,defaultValue="0") double distanceKm,@RequestParam(required=false) String selectedVehicleId,@RequestParam(defaultValue="false") boolean estimateAccepted,@RequestParam(required=false,defaultValue="0") double acceptedFare){
  UserAccount u=me(a);if(pickup.isBlank()||destination.isBlank()||pickup.length()>250||destination.length()>250||pickup.trim().equalsIgnoreCase(destination.trim()))return "redirect:/customer?error=locations";
  if(!List.of("CAR","AUTO","BIKE","OTHER").contains(category)||!List.of("IMMEDIATE","SCHEDULED").contains(tripType)||!List.of("CASH","UPI").contains(paymentMethod)||!List.of("ONE_WAY","ROUND_TRIP").contains(journeyType)||!Double.isFinite(distanceKm)||distanceKm<0||distanceKm>6000||!Double.isFinite(acceptedFare)||acceptedFare<0||acceptedFare>1000000||!estimateAccepted)return "redirect:/customer?error=details";
  ServiceCity pc=cities.findById(pickupCityId).filter(ServiceCity::isActive).orElse(null), dc=cities.findById(destinationCityId).filter(ServiceCity::isActive).orElse(null);if(pc==null||dc==null||!pc.getPinCodes().contains(pickupPinCode)||!dc.getPinCodes().contains(destinationPinCode))return "redirect:/customer?error=locationPin";
  if(pc.getId().equals(dc.getId())&&pickupPinCode.equals(destinationPinCode)&&pickup.trim().equalsIgnoreCase(destination.trim()))return "redirect:/customer?error=locations";
  RideBooking r=new RideBooking();r.setCustomerId(u.getId());r.setCustomerName(u.getName());r.setCustomerMobile(u.getMobile());r.setPickup(pickup.trim());r.setDestination(destination.trim());r.setPickupCityId(pc.getId());r.setPickupCity(pc.getCity());r.setPickupPinCode(pickupPinCode);r.setDestinationCityId(dc.getId());r.setDestinationCity(dc.getCity());r.setDestinationPinCode(destinationPinCode);r.setJourneyType(journeyType);r.setReturnLegStatus("ROUND_TRIP".equals(journeyType)?"REQUESTED":"NOT_APPLICABLE");r.setCategory(category);r.setTripType(tripType);r.setPaymentMethod(paymentMethod);
  RouteDistance configuredRoute=findRoute(pickupCityId,pickupPinCode,destinationCityId,destinationPinCode);
  double journeyMultiplier="ROUND_TRIP".equals(journeyType)?2:1;
  double oneWayDistance=configuredRoute!=null?configuredRoute.getDistanceKm():distanceKm/journeyMultiplier;
  double acceptedDistance=oneWayDistance*journeyMultiplier;
  if(!Double.isFinite(acceptedDistance)||acceptedDistance<=0||acceptedDistance>6000)return "redirect:/customer?error=distance";
  double calculatedFare=fareCalculator.estimate(fare(),category,oneWayDistance)*journeyMultiplier;
  if(!Double.isFinite(calculatedFare)||Math.abs(acceptedFare-calculatedFare)>0.01)return "redirect:/customer?error=estimateChanged";
  r.setEstimatedDistanceKm(acceptedDistance);r.setEstimatedFare(calculatedFare);r.setFareEstimateAccepted(true);r.setFareEstimateAcceptedAt(Instant.now());
  if("ROUND_TRIP".equals(journeyType)){try{LocalDateTime ret=LocalDateTime.parse(returnAt);if(ret.isBefore(LocalDateTime.now().plusMinutes(30))||ret.isAfter(LocalDateTime.now().plusDays(30)))return "redirect:/customer?error=returnDate";r.setReturnAt(ret);}catch(Exception ex){return "redirect:/customer?error=returnDate";}}
  if("SCHEDULED".equals(tripType)){try{LocalDateTime dt=LocalDateTime.parse(scheduledAt);if(dt.isBefore(LocalDateTime.now().plusMinutes(30))||dt.isAfter(LocalDateTime.now().plusDays(30)))return "redirect:/customer?error=schedule";r.setScheduledAt(dt);if("ROUND_TRIP".equals(journeyType)&&r.getReturnAt()!=null&&!r.getReturnAt().isAfter(dt))return "redirect:/customer?error=returnDate";}catch(Exception ex){return "redirect:/customer?error=schedule";}}
  String vehicleLockToken=null,driverLockToken=null,lockedVehicleId=null,lockedDriverId=null;
  try{
  if(selectedVehicleId!=null&&!selectedVehicleId.isBlank()){
   LocalDateTime startAt=r.getScheduledAt()!=null?r.getScheduledAt():LocalDateTime.now();
   LocalDateTime endAt=r.getReturnAt()!=null?r.getReturnAt().plusHours(4):startAt.plusHours(4);
   Vehicle selected=vehicles.findById(selectedVehicleId).filter(v->"APPROVED".equals(v.getStatus())&&"ACTIVE".equals(v.getOperationalStatus())&&category.equals(v.getCategory())&&documentsApproved(v)&&coversRoute(v,pickupCityId,pickupPinCode,destinationCityId,destinationPinCode,journeyType)&&vehicleAvailable(v,startAt,endAt)).orElse(null);
   if(selected==null)return "redirect:/customer?error=vehicleUnavailable";
   UserAccount owner=users.findById(selected.getOwnerId()).orElse(null);
   if(owner==null||!List.of("DRIVER","FLEET_OWNER").contains(owner.getRole())||!"APPROVED".equals(owner.getStatus())||!owner.isMobileVerified())return "redirect:/customer?error=vehicleUnavailable";
   String selectedAssignedDriverId=selected.getAssignedDriverId();UserAccount selectedDriver=availableDriverFor(selected,owner,startAt,endAt);
   if(selectedDriver==null)return "redirect:/customer?error=vehicleUnavailable";
   vehicleLockToken=reservationLocks.tryAcquire(selected.getId());if(vehicleLockToken==null)return "redirect:/customer?error=vehicleUnavailable";
   lockedVehicleId=selected.getId();
   if(selectedDriver.getId().equals(selected.getAssignedDriverId())){lockedDriverId=selectedDriver.getId();driverLockToken=reservationLocks.tryAcquireDriver(lockedDriverId);if(driverLockToken==null)return "redirect:/customer?error=vehicleUnavailable";}
   Vehicle lockedVehicle=vehicles.findById(lockedVehicleId).orElse(null);
   UserAccount lockedOwner=lockedVehicle==null?null:users.findById(lockedVehicle.getOwnerId()).orElse(null);
   UserAccount lockedDriver=lockedDriverId==null?null:users.findById(lockedDriverId).orElse(null);
   if(lockedVehicle==null||!Objects.equals(selectedAssignedDriverId,lockedVehicle.getAssignedDriverId())||!"APPROVED".equals(lockedVehicle.getStatus())||!"ACTIVE".equals(lockedVehicle.getOperationalStatus())||!category.equals(lockedVehicle.getCategory())||!documentsApproved(lockedVehicle)||!coversRoute(lockedVehicle,pickupCityId,pickupPinCode,destinationCityId,destinationPinCode,journeyType)||!vehicleAvailable(lockedVehicle,startAt,endAt)||lockedOwner==null||!List.of("DRIVER","FLEET_OWNER").contains(lockedOwner.getRole())||!"APPROVED".equals(lockedOwner.getStatus())||!lockedOwner.isMobileVerified()||availableDriverFor(lockedVehicle,lockedOwner,startAt,endAt)==null||(lockedDriverId!=null&&(lockedDriver==null||!driverAvailability.isAvailable(lockedDriver,startAt,endAt,null))))return "redirect:/customer?error=vehicleUnavailable";
   selected=lockedVehicle;
   r.setRequestedVehicleId(selected.getId());r.setRequestedVehicleNumber(selected.getRegistrationNumber());
  }
  r.setStatus("REQUESTED");
  rides.save(r);
  }finally{if(lockedDriverId!=null&&driverLockToken!=null)reservationLocks.releaseDriver(lockedDriverId,driverLockToken);if(lockedVehicleId!=null&&vehicleLockToken!=null)reservationLocks.release(lockedVehicleId,vehicleLockToken);}
  String msg="New "+r.getCategory()+" ride request: "+r.getPickupCity()+" to "+r.getDestinationCity()+". Estimated "+r.getEstimatedDistanceKm()+" km; fare ₹"+r.getEstimatedFare()+".";
  notifications.notifyAdmins("New ride request",msg,"/admin/rides",r.getId(),"RIDE_REQUEST");
  notifications.notifyUser(u.getId(),"Ride request received","Your ride request has been received. Estimated distance: "+r.getEstimatedDistanceKm()+" km; estimated fare: ₹"+r.getEstimatedFare()+". ASMJ Ride will confirm an approved driver and vehicle.","/customer",r.getId(),"RIDE_REQUEST_RECEIVED");
  LocalDateTime notifyStart=r.getScheduledAt()!=null?r.getScheduledAt():LocalDateTime.now();LocalDateTime notifyEnd=r.getReturnAt()!=null?r.getReturnAt().plusHours(4):notifyStart.plusHours(4);
  List<Vehicle> targets=(r.getRequestedVehicleId()!=null?vehicles.findById(r.getRequestedVehicleId()).map(List::of).orElse(List.of()):vehicles.findByStatus("APPROVED").stream().filter(v->{UserAccount owner=users.findById(v.getOwnerId()).orElse(null);return category.equals(v.getCategory())&&"ACTIVE".equals(v.getOperationalStatus())&&coversRoute(v,r.getPickupCityId(),r.getPickupPinCode(),r.getDestinationCityId(),r.getDestinationPinCode(),r.getJourneyType())&&documentsApproved(v)&&vehicleAvailable(v,notifyStart,notifyEnd)&&owner!=null&&List.of("DRIVER","FLEET_OWNER").contains(owner.getRole())&&"APPROVED".equals(owner.getStatus())&&owner.isMobileVerified()&&availableDriverFor(v,owner,notifyStart,notifyEnd)!=null;}).toList());
  Set<String> notified=new HashSet<>();
  for(Vehicle v:targets){
   if(v.getOwnerId()!=null&&notified.add(v.getOwnerId())){UserAccount owner=users.findById(v.getOwnerId()).orElse(null);String ownerLink=owner==null?"/partner":("FLEET_OWNER".equals(owner.getRole())?"/fleet":("DRIVER".equals(owner.getRole())?"/driver":"/partner"));notifications.notifyUser(v.getOwnerId(),"Ride request for your vehicle",msg,ownerLink,r.getId(),"RIDE_REQUEST");}
   if(v.getAssignedDriverId()!=null){UserAccount driver=users.findById(v.getAssignedDriverId()).orElse(null);if(driver!=null&&driver.isAvailable()&&"APPROVED".equals(driver.getStatus())&&driver.isMobileVerified()&&driverAvailability.isAvailable(driver,notifyStart,notifyEnd,r.getId())&&notified.add(driver.getId()))notifications.notifyUser(driver.getId(),"New ride available",msg,"/driver",r.getId(),"RIDE_REQUEST");}
   else {UserAccount owner=users.findById(v.getOwnerId()).orElse(null);if(owner!=null&&"DRIVER".equals(owner.getRole())&&owner.isAvailable()&&"APPROVED".equals(owner.getStatus())&&owner.isMobileVerified()&&driverAvailability.isAvailable(owner,notifyStart,notifyEnd,r.getId())&&notified.add(owner.getId()))notifications.notifyUser(owner.getId(),"New ride available",msg,"/driver",r.getId(),"RIDE_REQUEST");}
  }
  return "redirect:/customer?booked";
 }
 @GetMapping("/driver") String driver(Authentication a,Model m){UserAccount u=me(a);m.addAttribute("name",u.getName());m.addAttribute("user",u);m.addAttribute("partnerStatus",u.getStatus());m.addAttribute("available",u.isAvailable());m.addAttribute("rides",rides.findByStatusInOrderByCreatedAtDesc(List.of("REQUESTED","ACCEPTED","ASSIGNED","IN_PROGRESS")));m.addAttribute("myRides",rides.findByAssignedDriverIdOrderByCreatedAtDesc(u.getId()));m.addAttribute("returnRides",rides.findAll().stream().filter(x->u.getId().equals(x.getReturnAssignedDriverId())).toList());m.addAttribute("openReturnRides",rides.findAll().stream().filter(x->!"CANCELLED".equals(x.getStatus())&&"ROUND_TRIP".equals(x.getJourneyType())&&"REQUESTED".equals(x.getReturnLegStatus())&&x.getReturnAssignedDriverId()==null&&x.getReturnAt()!=null).toList());m.addAttribute("vehicles",eligibleVehicles(u));List<RideBooking> mine=rides.findByAssignedDriverIdOrderByCreatedAtDesc(u.getId());m.addAttribute("completedCount",mine.stream().filter(x->"COMPLETED".equals(x.getStatus())).count());double outwardEarnings=mine.stream().filter(x->"COMPLETED".equals(x.getStatus())).mapToDouble(x->x.getFinalFare()-("ROUND_TRIP".equals(x.getJourneyType())?x.getReturnFinalFare():0)).sum();double returnEarnings=rides.findAll().stream().filter(x->u.getId().equals(x.getReturnAssignedDriverId())&&"COMPLETED".equals(x.getReturnLegStatus())).mapToDouble(RideBooking::getReturnFinalFare).sum();m.addAttribute("earnings",outwardEarnings+returnEarnings);List<PaymentRecord> driverPayments=payments.findAll().stream().filter(p->u.getId().equals(p.getDriverId())||u.getId().equals(p.getReturnDriverId())).toList();double netPaid=driverPayments.stream().filter(p->"PAID".equals(p.getStatus())).mapToDouble(p->(u.getId().equals(p.getDriverId())?p.getDriverNetAmount():0)+(u.getId().equals(p.getReturnDriverId())?p.getReturnDriverNetAmount():0)).sum();double netSettled=driverPayments.stream().filter(p->"PAID".equals(p.getStatus())).mapToDouble(p->(u.getId().equals(p.getDriverId())&&p.isSettled()?p.getDriverNetAmount():0)+(u.getId().equals(p.getReturnDriverId())&&p.isReturnSettled()?p.getReturnDriverNetAmount():0)).sum();m.addAttribute("netPaid",netPaid);m.addAttribute("netSettled",netSettled);m.addAttribute("netPendingSettlement",Math.max(0,netPaid-netSettled));return "driver";}
 private List<Vehicle> eligibleVehicles(UserAccount u){List<Vehicle> out=new ArrayList<>();for(Vehicle v:vehicles.findByStatus("APPROVED"))if(u.getId().equals(v.getOwnerId())||(v.getOwnerId()!=null&&u.getId().equals(v.getAssignedDriverId())&&Objects.equals(u.getFleetOwnerId(),v.getOwnerId())))out.add(v);return out;}
 @GetMapping("/locations/cities") @ResponseBody List<Map<String,Object>> activeCities(){return cities.findByActiveTrueOrderByCityAsc().stream().map(c->{Map<String,Object>x=new LinkedHashMap<>();x.put("id",c.getId());x.put("city",c.getCity());return x;}).toList();}
 @GetMapping("/locations/cities/{id}/pins") @ResponseBody List<String> cityPins(@PathVariable String id){return cities.findById(id).filter(ServiceCity::isActive).map(ServiceCity::getPinCodes).orElse(List.of());}
 @PostMapping("/driver/availability") String availability(Authentication a,@RequestParam boolean available){UserAccount u=me(a);if(!u.isMobileVerified()||!"APPROVED".equals(u.getStatus())){u.setAvailable(false);users.save(u);return "redirect:/driver?approvalPending";}u.setAvailable(available);users.save(u);return "redirect:/driver";}
 @PostMapping("/rides/{id}/accept") String accept(Authentication a,@PathVariable String id,@RequestParam String vehicleId){
  UserAccount u=me(a);String rideLock=reservationLocks.tryAcquireRide(id);if(rideLock==null)return "redirect:/driver?error=taken";
  String vehicleLock=null,driverLock=null,lockedVehicleId=null;RideBooking r=null;Vehicle v=null;
  try{
   r=rides.findById(id).orElseThrow();if(!"REQUESTED".equals(r.getStatus()))return "redirect:/driver?error=taken";
   if(!u.isAvailable()||!u.isMobileVerified()||!"APPROVED".equals(u.getStatus()))return "redirect:/driver?error=unavailable";
   RideBooking requestedRide=r;v=eligibleVehicles(u).stream().filter(x->x.getId().equals(vehicleId)&&x.getCategory().equals(requestedRide.getCategory())&&"ACTIVE".equals(x.getOperationalStatus())&&documentsApproved(x)&&coversRoute(x,requestedRide.getPickupCityId(),requestedRide.getPickupPinCode(),requestedRide.getDestinationCityId(),requestedRide.getDestinationPinCode(),requestedRide.getJourneyType())&&(requestedRide.getRequestedVehicleId()==null||requestedRide.getRequestedVehicleId().equals(x.getId()))).findFirst().orElse(null);
   if(v==null)return "redirect:/driver?error=vehicle";
   LocalDateTime assignStart=r.getScheduledAt()!=null?r.getScheduledAt():LocalDateTime.now(),assignEnd=r.getReturnAt()!=null?r.getReturnAt().plusHours(4):assignStart.plusHours(4);
   lockedVehicleId=v.getId();vehicleLock=reservationLocks.tryAcquire(lockedVehicleId);if(vehicleLock==null)return "redirect:/driver?error=vehicleBusy";
   driverLock=reservationLocks.tryAcquireDriver(u.getId());if(driverLock==null)return "redirect:/driver?error=active";
   u=users.findById(u.getId()).orElseThrow();r=rides.findById(id).orElseThrow();v=vehicles.findById(v.getId()).orElse(null);
   if(!"REQUESTED".equals(r.getStatus())||v==null||!u.isAvailable()||!u.isMobileVerified()||!"APPROVED".equals(u.getStatus())||!driverAvailability.isAvailable(u,assignStart,assignEnd,r.getId())||!"APPROVED".equals(v.getStatus())||!"ACTIVE".equals(v.getOperationalStatus())||!documentsApproved(v)||!coversRoute(v,r.getPickupCityId(),r.getPickupPinCode(),r.getDestinationCityId(),r.getDestinationPinCode(),r.getJourneyType())||!r.getCategory().equals(v.getCategory())||(r.getRequestedVehicleId()!=null&&!r.getRequestedVehicleId().equals(v.getId()))||!vehicleAvailable(v,assignStart,assignEnd,r.getId()))return "redirect:/driver?error=vehicleBusy";
   UserAccount vehicleOwner=users.findById(v.getOwnerId()).orElse(null);if(vehicleOwner==null||!List.of("DRIVER","FLEET_OWNER").contains(vehicleOwner.getRole())||!"APPROVED".equals(vehicleOwner.getStatus())||!vehicleOwner.isMobileVerified())return "redirect:/driver?error=vehicle";
   r.setAssignedDriverId(u.getId());r.setAssignedDriverName(u.getName());r.setAssignedVehicleId(v.getId());r.setAssignedVehicleNumber(v.getRegistrationNumber());
   if("ROUND_TRIP".equals(r.getJourneyType())&&r.getReturnAssignedDriverId()==null){r.setReturnAssignedDriverId(u.getId());r.setReturnAssignedDriverName(u.getName());r.setReturnAssignedVehicleId(v.getId());r.setReturnAssignedVehicleNumber(v.getRegistrationNumber());}
   r.setPickupOtp(String.format("%06d",random.nextInt(1_000_000)));r.addStatusEvent("REQUESTED","ACCEPTED",u.getName(),"Driver accepted ride");r.setStatus("ACCEPTED");rides.save(r);
  }finally{if(driverLock!=null)reservationLocks.releaseDriver(u.getId(),driverLock);if(vehicleLock!=null&&lockedVehicleId!=null)reservationLocks.release(lockedVehicleId,vehicleLock);reservationLocks.releaseRide(id,rideLock);}
  notifications.notifyUser(r.getCustomerId(),"Driver assigned",u.getName()+" accepted your ride. Vehicle: "+r.getAssignedVehicleNumber()+".","/customer",r.getId(),"RIDE_ASSIGNED");return "redirect:/driver?accepted";
 }
 @PostMapping("/rides/{id}/status") String updateStatus(Authentication a,@PathVariable String id,@RequestParam String status,@RequestParam(required=false) String pickupOtp,@RequestParam(required=false,defaultValue="0") double actualDistanceKm,@RequestParam(required=false,defaultValue="0") int actualDurationMinutes,@RequestParam(required=false,defaultValue="0") int waitingMinutes){
  UserAccount u=me(a);String rideLock=reservationLocks.tryAcquireRide(id);if(rideLock==null)return "redirect:/driver?error=busy";RideBooking r=null;boolean completed=false;
  try{
   r=rides.findById(id).orElseThrow();if(!u.getId().equals(r.getAssignedDriverId()))return "redirect:/driver?error=denied";
   if("IN_PROGRESS".equals(status)){if(!("ACCEPTED".equals(r.getStatus())||"ASSIGNED".equals(r.getStatus()))||pickupOtp==null||!pickupOtp.equals(r.getPickupOtp()))return "redirect:/driver?error=otp";r.setTripStartedAt(Instant.now());r.addStatusEvent(r.getStatus(),"IN_PROGRESS",u.getName(),"Driver verified pickup OTP");r.setStatus("IN_PROGRESS");}
   else if("COMPLETED".equals(status)){if(!"IN_PROGRESS".equals(r.getStatus())||actualDistanceKm<0||actualDistanceKm>500||actualDurationMinutes<0||actualDurationMinutes>1440||waitingMinutes<0||waitingMinutes>1440)return "redirect:/driver?error=tripdata";r.setActualDistanceKm(actualDistanceKm);r.setActualDurationMinutes(actualDurationMinutes);r.setWaitingMinutes(waitingMinutes);r.setFinalFare(fareCalculator.finalFare(fare(),r.getCategory(),actualDistanceKm,actualDurationMinutes,waitingMinutes,r.getTripStartedAt()==null?null:LocalDateTime.ofInstant(r.getTripStartedAt(),ZoneId.systemDefault())));r.setTripCompletedAt(Instant.now());r.setPickupOtp(null);r.addStatusEvent(r.getStatus(),"COMPLETED",u.getName(),"Driver completed trip");r.setStatus("COMPLETED");completed=true;}
   else return "redirect:/driver";rides.save(r);
  }finally{reservationLocks.releaseRide(id,rideLock);}
  if(completed)notifications.notifyUser(r.getCustomerId(),"Outward journey completed","Your outward journey is complete. "+("ROUND_TRIP".equals(r.getJourneyType())?"Your return journey remains "+r.getReturnLegStatus()+".":"Final fare: ₹"+r.getFinalFare()+"."),"/customer",r.getId(),"RIDE_COMPLETED");
  return "redirect:/driver?updated";
 }
 @PostMapping("/rides/{id}/return/accept") String acceptReturn(Authentication a,@PathVariable String id,@RequestParam String vehicleId){
  UserAccount u=me(a);String rideLock=reservationLocks.tryAcquireRide(id);if(rideLock==null)return "redirect:/driver?error=returnTaken";
  String vehicleLock=null,driverLock=null,lockedVehicleId=null;RideBooking r=null;Vehicle v=null;
  try{
   r=rides.findById(id).orElseThrow();if("CANCELLED".equals(r.getStatus())||!"ROUND_TRIP".equals(r.getJourneyType())||!"REQUESTED".equals(r.getReturnLegStatus())||r.getReturnAt()==null||r.getReturnAssignedDriverId()!=null)return "redirect:/driver?error=returnTaken";
   if(!u.isAvailable()||!u.isMobileVerified()||!"APPROVED".equals(u.getStatus()))return "redirect:/driver?error=unavailable";
   RideBooking requestedRide=r;v=eligibleVehicles(u).stream().filter(x->x.getId().equals(vehicleId)&&x.getCategory().equals(requestedRide.getCategory())&&"APPROVED".equals(x.getStatus())&&"ACTIVE".equals(x.getOperationalStatus())&&documentsApproved(x)&&coversReturnRoute(x,requestedRide)).findFirst().orElse(null);
   if(v==null)return "redirect:/driver?error=vehicle";
   LocalDateTime returnStart=r.getReturnAt(),returnEnd=returnStart.plusHours(4);
   lockedVehicleId=v.getId();vehicleLock=reservationLocks.tryAcquire(lockedVehicleId);if(vehicleLock==null)return "redirect:/driver?error=vehicleBusy";
   driverLock=reservationLocks.tryAcquireDriver(u.getId());if(driverLock==null)return "redirect:/driver?error=active";
   u=users.findById(u.getId()).orElseThrow();r=rides.findById(id).orElseThrow();v=vehicles.findById(lockedVehicleId).orElse(null);
   UserAccount vehicleOwner=v==null?null:users.findById(v.getOwnerId()).orElse(null);
   if(v==null||vehicleOwner==null||!List.of("DRIVER","FLEET_OWNER").contains(vehicleOwner.getRole())||!"APPROVED".equals(vehicleOwner.getStatus())||!vehicleOwner.isMobileVerified()||"CANCELLED".equals(r.getStatus())||!"ROUND_TRIP".equals(r.getJourneyType())||!"REQUESTED".equals(r.getReturnLegStatus())||r.getReturnAssignedDriverId()!=null||r.getReturnAt()==null||!u.isAvailable()||!u.isMobileVerified()||!"APPROVED".equals(u.getStatus())||!driverAvailability.isAvailable(u,returnStart,returnEnd,r.getId())||!"APPROVED".equals(v.getStatus())||!"ACTIVE".equals(v.getOperationalStatus())||!documentsApproved(v)||!coversReturnRoute(v,r)||!r.getCategory().equals(v.getCategory())||!vehicleAvailable(v,returnStart,returnEnd,r.getId()))return "redirect:/driver?error=vehicleBusy";
   r.setReturnAssignedDriverId(u.getId());r.setReturnAssignedDriverName(u.getName());r.setReturnAssignedVehicleId(v.getId());r.setReturnAssignedVehicleNumber(v.getRegistrationNumber());r.setReturnPickupOtp(String.format("%06d",random.nextInt(1_000_000)));r.addStatusEvent("RETURN_REQUESTED","RETURN_ACCEPTED",u.getName(),"Driver accepted return leg");r.setReturnLegStatus("ACCEPTED");rides.save(r);
  }finally{if(driverLock!=null)reservationLocks.releaseDriver(u.getId(),driverLock);if(vehicleLock!=null&&lockedVehicleId!=null)reservationLocks.release(lockedVehicleId,vehicleLock);reservationLocks.releaseRide(id,rideLock);}
  notifications.notifyUser(r.getCustomerId(),"Return driver assigned",u.getName()+" accepted your return journey. Vehicle: "+r.getReturnAssignedVehicleNumber()+".","/customer",r.getId(),"RETURN_ASSIGNED");return "redirect:/driver?returnAccepted";
 }
 @PostMapping("/rides/{id}/return/confirm") String confirmPreferredReturn(Authentication a,@PathVariable String id){
  UserAccount u=me(a);String rideLock=reservationLocks.tryAcquireRide(id);if(rideLock==null)return "redirect:/driver?error=returnNotReady";
  String vehicleLock=null,driverLock=null,lockedVehicleId=null;RideBooking r=null;
  try{
   r=rides.findById(id).orElseThrow();if(!u.getId().equals(r.getReturnAssignedDriverId())||!"ROUND_TRIP".equals(r.getJourneyType())||!"COMPLETED".equals(r.getStatus())||!"REQUESTED".equals(r.getReturnLegStatus())||r.getReturnAt()==null||r.getReturnAt().isAfter(LocalDateTime.now())||!u.isMobileVerified()||!"APPROVED".equals(u.getStatus())||!u.isAvailable())return "redirect:/driver?error=returnNotReady";
   lockedVehicleId=r.getReturnAssignedVehicleId();Vehicle v=lockedVehicleId==null?null:vehicles.findById(lockedVehicleId).orElse(null);
   if(v==null||!"APPROVED".equals(v.getStatus())||!"ACTIVE".equals(v.getOperationalStatus())||!documentsApproved(v)||!coversReturnRoute(v,r)||!r.getCategory().equals(v.getCategory()))return "redirect:/driver?error=vehicle";
   LocalDateTime returnStart=r.getReturnAt(),returnEnd=returnStart.plusHours(4);
   vehicleLock=reservationLocks.tryAcquire(lockedVehicleId);if(vehicleLock==null)return "redirect:/driver?error=vehicleBusy";
   driverLock=reservationLocks.tryAcquireDriver(u.getId());if(driverLock==null)return "redirect:/driver?error=active";
   u=users.findById(u.getId()).orElseThrow();r=rides.findById(id).orElseThrow();v=vehicles.findById(lockedVehicleId).orElse(null);
   UserAccount vehicleOwner=v==null?null:users.findById(v.getOwnerId()).orElse(null);
   if(v==null||vehicleOwner==null||!List.of("DRIVER","FLEET_OWNER").contains(vehicleOwner.getRole())||!"APPROVED".equals(vehicleOwner.getStatus())||!vehicleOwner.isMobileVerified()||!u.getId().equals(r.getReturnAssignedDriverId())||!"COMPLETED".equals(r.getStatus())||!"REQUESTED".equals(r.getReturnLegStatus())||r.getReturnAt()==null||r.getReturnAt().isAfter(LocalDateTime.now())||!driverAvailability.isAvailable(u,returnStart,returnEnd,r.getId())||!"APPROVED".equals(v.getStatus())||!"ACTIVE".equals(v.getOperationalStatus())||!documentsApproved(v)||!coversReturnRoute(v,r)||!r.getCategory().equals(v.getCategory())||!vehicleAvailable(v,returnStart,returnEnd,r.getId()))return "redirect:/driver?error=vehicle";
   r.setReturnPickupOtp(String.format("%06d",random.nextInt(1_000_000)));r.addStatusEvent("RETURN_REQUESTED","RETURN_ACCEPTED",u.getName(),"Driver confirmed preferred return assignment");r.setReturnLegStatus("ACCEPTED");rides.save(r);
  }finally{if(driverLock!=null)reservationLocks.releaseDriver(u.getId(),driverLock);if(vehicleLock!=null&&lockedVehicleId!=null)reservationLocks.release(lockedVehicleId,vehicleLock);reservationLocks.releaseRide(id,rideLock);}
  notifications.notifyUser(r.getCustomerId(),"Return driver confirmed",u.getName()+" is confirmed for your return journey.","/customer",r.getId(),"RETURN_ASSIGNED");return "redirect:/driver?returnAccepted";
 }
 @PostMapping("/rides/{id}/return/status") String returnStatus(Authentication a,@PathVariable String id,@RequestParam String status,@RequestParam(required=false) String pickupOtp,@RequestParam(required=false,defaultValue="0") double actualDistanceKm){
  UserAccount u=me(a);String rideLock=reservationLocks.tryAcquireRide(id);if(rideLock==null)return "redirect:/driver?error=busy";RideBooking r=null;boolean completed=false;
  try{
   r=rides.findById(id).orElseThrow();if(!u.getId().equals(r.getReturnAssignedDriverId()))return "redirect:/driver?error=denied";
   if("IN_PROGRESS".equals(status)){if(!"COMPLETED".equals(r.getStatus())||r.getReturnAt()==null||r.getReturnAt().isAfter(LocalDateTime.now())||!"ACCEPTED".equals(r.getReturnLegStatus())||pickupOtp==null||!pickupOtp.equals(r.getReturnPickupOtp()))return "redirect:/driver?error=otp";r.setReturnTripStartedAt(Instant.now());r.addStatusEvent("RETURN_ACCEPTED","RETURN_IN_PROGRESS",u.getName(),"Return pickup OTP verified");r.setReturnLegStatus("IN_PROGRESS");}
   else if("COMPLETED".equals(status)){if(!"IN_PROGRESS".equals(r.getReturnLegStatus())||actualDistanceKm<=0||actualDistanceKm>1000)return "redirect:/driver?error=tripdata";r.setReturnActualDistanceKm(actualDistanceKm);r.setReturnFinalFare(fareCalculator.estimate(fare(),r.getCategory(),actualDistanceKm));r.setReturnTripCompletedAt(Instant.now());r.setReturnPickupOtp(null);r.addStatusEvent("RETURN_IN_PROGRESS","RETURN_COMPLETED",u.getName(),"Return journey completed");r.setReturnLegStatus("COMPLETED");r.setFinalFare(r.getFinalFare()+r.getReturnFinalFare());completed=true;}
   else return "redirect:/driver";rides.save(r);
  }finally{reservationLocks.releaseRide(id,rideLock);}
  if(completed)notifications.notifyUser(r.getCustomerId(),"Return journey completed","Your return journey is complete. Final estimated fare: ₹"+r.getFinalFare()+".","/customer",r.getId(),"RETURN_COMPLETED");
  return "redirect:/driver?returnUpdated";
 }
 @PostMapping("/driver/location") String location(Authentication a,@RequestParam double latitude,@RequestParam double longitude){UserAccount u=me(a);if(latitude < -90||latitude>90||longitude < -180||longitude>180)return "redirect:/driver?error=location";for(RideBooking r:rides.findByAssignedDriverIdOrderByCreatedAtDesc(u.getId()))if("IN_PROGRESS".equals(r.getStatus())||"ACCEPTED".equals(r.getStatus())||"ASSIGNED".equals(r.getStatus())){r.setCurrentDriverLatitude(latitude);r.setCurrentDriverLongitude(longitude);r.setDriverLocationUpdatedAt(Instant.now());rides.save(r);}return "redirect:/driver?location=updated";}
 @GetMapping("/rides/{id}/location") @ResponseBody ResponseEntity<Map<String,Object>> locationForCustomer(Authentication a,@PathVariable String id){UserAccount u=me(a);RideBooking r=rides.findById(id).orElseThrow();if(!u.getId().equals(r.getCustomerId()))return ResponseEntity.status(403).build();Map<String,Object> data=new LinkedHashMap<>();data.put("status",r.getStatus());data.put("latitude",r.getCurrentDriverLatitude());data.put("longitude",r.getCurrentDriverLongitude());data.put("updatedAt",r.getDriverLocationUpdatedAt());data.put("driver",r.getAssignedDriverName());return ResponseEntity.ok(data);}
 @GetMapping("/admin/rides") String adminRides(Model m){m.addAttribute("rides",rides.findAll());m.addAttribute("drivers",users.findByRoleAndStatusAndMobileVerified("DRIVER","APPROVED",true));m.addAttribute("vehicles",vehicles.findByStatus("APPROVED"));return "admin-rides";}
 @PostMapping("/admin/rides/{id}/cancel") String cancel(@PathVariable String id){
  String rideLock=reservationLocks.tryAcquireRide(id);if(rideLock==null)return "redirect:/admin/rides?error=busy";RideBooking r=null;boolean cancelled=false;
  try{r=rides.findById(id).orElseThrow();if(!List.of("COMPLETED","CANCELLED").contains(r.getStatus())){if(List.of("ACCEPTED","IN_PROGRESS").contains(r.getStatus()))r.setCancellationCharge(fare().getCancellationFee());String old=r.getStatus();r.setStatus("CANCELLED");r.setPickupOtp(null);r.setReturnPickupOtp(null);r.addStatusEvent(old,"CANCELLED","ADMIN","Admin cancelled ride");if("ROUND_TRIP".equals(r.getJourneyType())&&List.of("REQUESTED","ACCEPTED").contains(r.getReturnLegStatus()))r.setReturnLegStatus("CANCELLED");rides.save(r);cancelled=true;}}finally{reservationLocks.releaseRide(id,rideLock);}
  if(cancelled)notifications.notifyUser(r.getCustomerId(),"Ride cancelled","Your ride was cancelled by ASMJ Ride administration.","/customer",r.getId(),"RIDE_CANCELLED");return "redirect:/admin/rides";
 }
 @PostMapping("/rides/{id}/cancel") String customerCancel(Authentication a,@PathVariable String id){
  UserAccount u=me(a);String rideLock=reservationLocks.tryAcquireRide(id);if(rideLock==null)return "redirect:/customer?error=busy";RideBooking r=null;boolean cancelled=false;
  try{r=rides.findById(id).orElseThrow();if(u.getId().equals(r.getCustomerId())&&List.of("REQUESTED","ACCEPTED","ASSIGNED").contains(r.getStatus())){if("ACCEPTED".equals(r.getStatus()))r.setCancellationCharge(fare().getCancellationFee());r.setPickupOtp(null);r.setReturnPickupOtp(null);r.addStatusEvent(r.getStatus(),"CANCELLED",u.getName(),"Customer cancelled booking");r.setStatus("CANCELLED");if("ROUND_TRIP".equals(r.getJourneyType())&&List.of("REQUESTED","ACCEPTED").contains(r.getReturnLegStatus()))r.setReturnLegStatus("CANCELLED");rides.save(r);cancelled=true;}}finally{reservationLocks.releaseRide(id,rideLock);}
  if(cancelled)notifications.notifyAdmins("Customer cancelled ride","Customer "+u.getName()+" cancelled ride "+id+".","/admin/rides",id,"RIDE_CANCELLED");return "redirect:/customer";
 }
 @PostMapping("/admin/rides/{id}/return/assign") String adminAssignReturn(@PathVariable String id,@RequestParam String driverId,@RequestParam String vehicleId){
  String rideLock=reservationLocks.tryAcquireRide(id);if(rideLock==null)return "redirect:/admin/rides?error=returnAssign";
  String vehicleLock=null,driverLock=null,lockedVehicleId=null;RideBooking r=null;UserAccount d=users.findById(driverId).orElse(null);Vehicle v=null;
  try{
   r=rides.findById(id).orElseThrow();boolean sameOutward=d!=null&&d.getId().equals(r.getAssignedDriverId());
   if(!"ROUND_TRIP".equals(r.getJourneyType())||r.getReturnAt()==null||"CANCELLED".equals(r.getStatus())||List.of("IN_PROGRESS","COMPLETED","CANCELLED").contains(r.getReturnLegStatus())||d==null||!"DRIVER".equals(d.getRole())||!"APPROVED".equals(d.getStatus())||!d.isMobileVerified())return "redirect:/admin/rides?error=returnAssign";
   v=vehicles.findById(vehicleId).orElse(null);if(v==null||(!d.getId().equals(v.getOwnerId())&&!d.getId().equals(v.getAssignedDriverId())&&!Objects.equals(d.getFleetOwnerId(),v.getOwnerId()))||!"APPROVED".equals(v.getStatus())||!"ACTIVE".equals(v.getOperationalStatus())||!documentsApproved(v)||!coversReturnRoute(v,r)||!r.getCategory().equals(v.getCategory()))return "redirect:/admin/rides?error=returnAssign";
   LocalDateTime returnStart=r.getReturnAt(),returnEnd=returnStart.plusHours(4);
   if(sameOutward?driverAvailability.hasConflict(d,returnStart,returnEnd,r.getId()):!driverAvailability.isAvailable(d,returnStart,returnEnd,r.getId()))return "redirect:/admin/rides?error=returnAssign";
   lockedVehicleId=v.getId();vehicleLock=reservationLocks.tryAcquire(lockedVehicleId);if(vehicleLock==null)return "redirect:/admin/rides?error=returnAssign";
   driverLock=reservationLocks.tryAcquireDriver(d.getId());if(driverLock==null)return "redirect:/admin/rides?error=returnAssign";
   d=users.findById(driverId).orElseThrow();r=rides.findById(id).orElseThrow();v=vehicles.findById(lockedVehicleId).orElse(null);sameOutward=d.getId().equals(r.getAssignedDriverId());
   UserAccount vehicleOwner=v==null?null:users.findById(v.getOwnerId()).orElse(null);if(v==null||vehicleOwner==null||!List.of("DRIVER","FLEET_OWNER").contains(vehicleOwner.getRole())||!"APPROVED".equals(vehicleOwner.getStatus())||!vehicleOwner.isMobileVerified()||!"ROUND_TRIP".equals(r.getJourneyType())||r.getReturnAt()==null||"CANCELLED".equals(r.getStatus())||List.of("IN_PROGRESS","COMPLETED","CANCELLED").contains(r.getReturnLegStatus())||!"APPROVED".equals(d.getStatus())||!d.isMobileVerified()||(sameOutward?driverAvailability.hasConflict(d,returnStart,returnEnd,r.getId()):!driverAvailability.isAvailable(d,returnStart,returnEnd,r.getId()))||!"APPROVED".equals(v.getStatus())||!"ACTIVE".equals(v.getOperationalStatus())||!documentsApproved(v)||!coversReturnRoute(v,r)||!r.getCategory().equals(v.getCategory())||!vehicleAvailable(v,returnStart,returnEnd,r.getId()))return "redirect:/admin/rides?error=returnAssign";
   r.setReturnAssignedDriverId(d.getId());r.setReturnAssignedDriverName(d.getName());r.setReturnAssignedVehicleId(v.getId());r.setReturnAssignedVehicleNumber(v.getRegistrationNumber());r.setReturnPickupOtp(String.format("%06d",random.nextInt(1_000_000)));String oldReturn=r.getReturnLegStatus();r.setReturnLegStatus("ACCEPTED");r.addStatusEvent("RETURN_"+oldReturn,"RETURN_ACCEPTED","ADMIN","Admin assigned return driver and vehicle");rides.save(r);
  }finally{if(driverLock!=null&&d!=null)reservationLocks.releaseDriver(d.getId(),driverLock);if(vehicleLock!=null&&lockedVehicleId!=null)reservationLocks.release(lockedVehicleId,vehicleLock);reservationLocks.releaseRide(id,rideLock);}
  notifications.notifyUser(r.getCustomerId(),"Return driver assigned",d.getName()+" has been assigned to your return journey. Vehicle: "+r.getReturnAssignedVehicleNumber()+".","/customer",r.getId(),"RETURN_ASSIGNED");return "redirect:/admin/rides?returnAssigned";
 }
 @PostMapping("/admin/rides/{id}/assign-outward") String adminAssignOutward(@PathVariable String id,@RequestParam String driverId,@RequestParam String vehicleId,@RequestParam(required=false) String note){
  String rideLock=reservationLocks.tryAcquireRide(id);if(rideLock==null)return "redirect:/admin/rides?error=assign";
  String vehicleLock=null,driverLock=null,lockedVehicleId=null;RideBooking r=null;UserAccount d=users.findById(driverId).orElse(null);Vehicle v=null;
  try{
   r=rides.findById(id).orElseThrow();if(d==null||!"DRIVER".equals(d.getRole())||!"APPROVED".equals(d.getStatus())||!d.isMobileVerified()||List.of("IN_PROGRESS","COMPLETED","CANCELLED").contains(r.getStatus()))return "redirect:/admin/rides?error=assign";
   v=vehicles.findById(vehicleId).orElse(null);if(v==null||(!d.getId().equals(v.getOwnerId())&&!d.getId().equals(v.getAssignedDriverId())&&!Objects.equals(d.getFleetOwnerId(),v.getOwnerId()))||!"APPROVED".equals(v.getStatus())||!"ACTIVE".equals(v.getOperationalStatus())||!documentsApproved(v)||!coversRoute(v,r.getPickupCityId(),r.getPickupPinCode(),r.getDestinationCityId(),r.getDestinationPinCode(),r.getJourneyType())||!r.getCategory().equals(v.getCategory())||(r.getRequestedVehicleId()!=null&&!r.getRequestedVehicleId().equals(v.getId())))return "redirect:/admin/rides?error=assign";
   LocalDateTime assignStart=r.getScheduledAt()!=null?r.getScheduledAt():LocalDateTime.now(),assignEnd=r.getReturnAt()!=null?r.getReturnAt().plusHours(4):assignStart.plusHours(4);
   boolean sameCurrent=d.getId().equals(r.getAssignedDriverId());if(sameCurrent?driverAvailability.hasConflict(d,assignStart,assignEnd,r.getId()):!driverAvailability.isAvailable(d,assignStart,assignEnd,r.getId()))return "redirect:/admin/rides?error=assign";
   lockedVehicleId=v.getId();vehicleLock=reservationLocks.tryAcquire(lockedVehicleId);if(vehicleLock==null)return "redirect:/admin/rides?error=vehicleBusy";
   driverLock=reservationLocks.tryAcquireDriver(d.getId());if(driverLock==null)return "redirect:/admin/rides?error=driverBusy";
   d=users.findById(driverId).orElseThrow();r=rides.findById(id).orElseThrow();v=vehicles.findById(lockedVehicleId).orElse(null);sameCurrent=d.getId().equals(r.getAssignedDriverId());
   UserAccount vehicleOwner=v==null?null:users.findById(v.getOwnerId()).orElse(null);if(v==null||vehicleOwner==null||!List.of("DRIVER","FLEET_OWNER").contains(vehicleOwner.getRole())||!"APPROVED".equals(vehicleOwner.getStatus())||!vehicleOwner.isMobileVerified()||List.of("IN_PROGRESS","COMPLETED","CANCELLED").contains(r.getStatus())||!"APPROVED".equals(d.getStatus())||!d.isMobileVerified()||(sameCurrent?driverAvailability.hasConflict(d,assignStart,assignEnd,r.getId()):!driverAvailability.isAvailable(d,assignStart,assignEnd,r.getId()))||!"APPROVED".equals(v.getStatus())||!"ACTIVE".equals(v.getOperationalStatus())||!documentsApproved(v)||!coversRoute(v,r.getPickupCityId(),r.getPickupPinCode(),r.getDestinationCityId(),r.getDestinationPinCode(),r.getJourneyType())||!r.getCategory().equals(v.getCategory())||(r.getRequestedVehicleId()!=null&&!r.getRequestedVehicleId().equals(v.getId()))||!vehicleAvailable(v,assignStart,assignEnd,r.getId()))return "redirect:/admin/rides?error=vehicleBusy";
   String old=r.getStatus();boolean preferredReturn="ROUND_TRIP".equals(r.getJourneyType())&&"REQUESTED".equals(r.getReturnLegStatus())&&(r.getReturnAssignedDriverId()==null||(Objects.equals(r.getReturnAssignedDriverId(),r.getAssignedDriverId())&&Objects.equals(r.getReturnAssignedVehicleId(),r.getAssignedVehicleId())));
   r.setAssignedDriverId(d.getId());r.setAssignedDriverName(d.getName());r.setAssignedVehicleId(v.getId());r.setAssignedVehicleNumber(v.getRegistrationNumber());if(preferredReturn){r.setReturnAssignedDriverId(d.getId());r.setReturnAssignedDriverName(d.getName());r.setReturnAssignedVehicleId(v.getId());r.setReturnAssignedVehicleNumber(v.getRegistrationNumber());}
   r.setPickupOtp(String.format("%06d",random.nextInt(1_000_000)));r.setStatus("ACCEPTED");r.addStatusEvent(old,"ACCEPTED","ADMIN",note==null?"Admin assigned outward driver and vehicle":note);rides.save(r);
  }finally{if(driverLock!=null&&d!=null)reservationLocks.releaseDriver(d.getId(),driverLock);if(vehicleLock!=null&&lockedVehicleId!=null)reservationLocks.release(lockedVehicleId,vehicleLock);reservationLocks.releaseRide(id,rideLock);}
  notifications.notifyUser(r.getCustomerId(),"Driver assigned",d.getName()+" has been assigned to your ride. Vehicle: "+r.getAssignedVehicleNumber()+".","/customer",r.getId(),"RIDE_ASSIGNED");return "redirect:/admin/rides?assigned";
 }
 @PostMapping("/admin/rides/{id}/status") String adminTripStatus(@PathVariable String id,@RequestParam String status,@RequestParam String reason){
  String rideLock=reservationLocks.tryAcquireRide(id);if(rideLock==null)return "redirect:/admin/rides?error=busy";RideBooking r=null;boolean cancelled=false;
  try{r=rides.findById(id).orElseThrow();if(reason==null||reason.isBlank()||reason.length()>300||!List.of("REQUESTED","ACCEPTED","ASSIGNED","CANCELLED").contains(status)||List.of("IN_PROGRESS","COMPLETED","CANCELLED").contains(r.getStatus())&&!("CANCELLED".equals(status)&&!"CANCELLED".equals(r.getStatus())))return "redirect:/admin/rides?error=status";String old=r.getStatus();boolean validTransition=("REQUESTED".equals(old)&&List.of("REQUESTED","ACCEPTED","ASSIGNED","CANCELLED").contains(status))||("ACCEPTED".equals(old)&&List.of("ACCEPTED","ASSIGNED","CANCELLED").contains(status))||("ASSIGNED".equals(old)&&List.of("ASSIGNED","CANCELLED").contains(status))||("IN_PROGRESS".equals(old)&&"CANCELLED".equals(status));if(!validTransition)return "redirect:/admin/rides?error=status";if(("ACCEPTED".equals(status)||"ASSIGNED".equals(status))&&r.getAssignedDriverId()==null)return "redirect:/admin/rides?error=status";if("CANCELLED".equals(status)){r.setCancellationCharge(List.of("ACCEPTED","ASSIGNED","IN_PROGRESS").contains(old)?fare().getCancellationFee():0);r.setPickupOtp(null);r.setReturnPickupOtp(null);if("ROUND_TRIP".equals(r.getJourneyType())&&List.of("REQUESTED","ACCEPTED").contains(r.getReturnLegStatus()))r.setReturnLegStatus("CANCELLED");cancelled=true;}r.setStatus(status);r.addStatusEvent(old,status,"ADMIN",reason.trim());rides.save(r);}finally{reservationLocks.releaseRide(id,rideLock);}
  if(cancelled)notifications.notifyUser(r.getCustomerId(),"Ride cancelled","Your ride was cancelled by ASMJ Ride administration. Reason: "+reason.trim(),"/customer",r.getId(),"RIDE_CANCELLED");return "redirect:/admin/rides?statusUpdated";
 }
 @PostMapping("/admin/rides/{id}/return/status") String adminReturnStatus(@PathVariable String id,@RequestParam String status,@RequestParam String reason){
  String rideLock=reservationLocks.tryAcquireRide(id);if(rideLock==null)return "redirect:/admin/rides?error=busy";RideBooking r=null;boolean cancelled=false;
  try{r=rides.findById(id).orElseThrow();if(!"ROUND_TRIP".equals(r.getJourneyType())||reason==null||reason.isBlank()||reason.length()>300||!List.of("REQUESTED","ACCEPTED","CANCELLED").contains(status)||List.of("IN_PROGRESS","COMPLETED","CANCELLED").contains(r.getReturnLegStatus()))return "redirect:/admin/rides?error=returnStatus";if("ACCEPTED".equals(status)&&r.getReturnAssignedDriverId()==null)return "redirect:/admin/rides?error=returnStatus";String old=r.getReturnLegStatus();r.setReturnLegStatus(status);if("CANCELLED".equals(status)){r.setReturnPickupOtp(null);cancelled=true;}r.addStatusEvent("RETURN_"+old,"RETURN_"+status,"ADMIN",reason.trim());rides.save(r);}finally{reservationLocks.releaseRide(id,rideLock);}
  if(cancelled)notifications.notifyUser(r.getCustomerId(),"Return journey cancelled","Your return journey was cancelled by ASMJ Ride administration. Reason: "+reason.trim(),"/customer",r.getId(),"RETURN_CANCELLED");return "redirect:/admin/rides?statusUpdated";
 }
 @GetMapping("/admin/fares") String fareSettings(Model m){m.addAttribute("fare",fare());return "admin-fares";}
 @PostMapping("/admin/fares") String saveFares(@RequestParam double baseFare,@RequestParam double perKm,@RequestParam double perMinute,@RequestParam double minimumFare,@RequestParam double waitingPerMinute,@RequestParam double nightMultiplier,@RequestParam int nightStartHour,@RequestParam int nightEndHour,@RequestParam double cancellationFee,@RequestParam double carMultiplier,@RequestParam double autoMultiplier,@RequestParam double bikeMultiplier,@RequestParam double otherMultiplier,@RequestParam(defaultValue="15") double individualCommissionPercent,@RequestParam(defaultValue="10") double fleetCommissionPercent){if(baseFare<0||baseFare>10000||perKm<0||perKm>1000||perMinute<0||perMinute>500||minimumFare<0||minimumFare>10000||waitingPerMinute<0||waitingPerMinute>500||nightMultiplier<1||nightMultiplier>5||nightStartHour<0||nightStartHour>23||nightEndHour<0||nightEndHour>23||cancellationFee<0||cancellationFee>5000||!validMultiplier(carMultiplier)||!validMultiplier(autoMultiplier)||!validMultiplier(bikeMultiplier)||!validMultiplier(otherMultiplier)||individualCommissionPercent<0||individualCommissionPercent>50||fleetCommissionPercent<0||fleetCommissionPercent>50)return "redirect:/admin/fares?error=values";FareRule f=fare();f.setBaseFare(baseFare);f.setPerKm(perKm);f.setPerMinute(perMinute);f.setMinimumFare(minimumFare);f.setWaitingPerMinute(waitingPerMinute);f.setNightMultiplier(nightMultiplier);f.setNightStartHour(nightStartHour);f.setNightEndHour(nightEndHour);f.setCancellationFee(cancellationFee);f.setCarMultiplier(carMultiplier);f.setAutoMultiplier(autoMultiplier);f.setBikeMultiplier(bikeMultiplier);f.setOtherMultiplier(otherMultiplier);f.setIndividualCommissionPercent(individualCommissionPercent);f.setFleetCommissionPercent(fleetCommissionPercent);f.touch();fareRules.save(f);return "redirect:/admin/fares?saved";}
 private boolean validMultiplier(double v){return v>=0.1&&v<=5;}
}
