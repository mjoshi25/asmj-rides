package in.asmj.ride.web;

import in.asmj.ride.model.*;import in.asmj.ride.repo.*;import in.asmj.ride.service.CloudinaryUploadService;import in.asmj.ride.service.DocumentRequirementService;
import org.springframework.http.*;import org.springframework.security.core.Authentication;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;import org.springframework.core.io.ClassPathResource;
import java.util.*;import java.net.URI;import java.net.http.HttpClient;import java.net.http.HttpRequest;import java.net.http.HttpResponse;import java.time.Duration;

@Controller
public class AssetController {
 private final UserAccountRepository users;private final VehicleRepository vehicles;private final ServiceCityRepository cities;private final PaymentRecordRepository payments;private final SupportTicketRepository tickets;private final SafetyReportRepository safety;private final CloudinaryUploadService cloud;private final DocumentRequirementService documentRequirements;
 public AssetController(UserAccountRepository u,VehicleRepository v,ServiceCityRepository cities,PaymentRecordRepository p,SupportTicketRepository t,SafetyReportRepository s,CloudinaryUploadService c,DocumentRequirementService documentRequirements){users=u;vehicles=v;this.cities=cities;payments=p;tickets=t;safety=s;cloud=c;this.documentRequirements=documentRequirements;}
 private UserAccount me(Authentication a){return users.findByEmailIgnoreCase(a.getName()).orElseThrow();}
 @GetMapping("/admin/uploads") public String uploads(Model m){List<UserAccount> allUsers=users.findAll();List<Vehicle> allVehicles=vehicles.findAll();m.addAttribute("users",allUsers);m.addAttribute("vehicles",allVehicles);Map<String,UserAccount> vehicleOwners=new HashMap<>();for(Vehicle vehicle:allVehicles){if(vehicle.getOwnerId()!=null)users.findById(vehicle.getOwnerId()).ifPresent(owner->vehicleOwners.put(vehicle.getId(),owner));}m.addAttribute("vehicleOwners",vehicleOwners);m.addAttribute("cities",cities.findByActiveTrueOrderByCityAsc());m.addAttribute("payments",payments.findAllByOrderByCreatedAtDesc());m.addAttribute("tickets",tickets.findAllByOrderByCreatedAtDesc());m.addAttribute("safetyReports",safety.findAllByOrderByCreatedAtDesc());m.addAttribute("cloudinaryReady",cloud.configured());return "admin-uploads";}
 @PostMapping("/admin/uploads/{kind}/{ownerId}/{assetId}/verify") public String verify(@PathVariable String kind,@PathVariable String ownerId,@PathVariable String assetId,@RequestParam String decision,@RequestParam(required=false)String note){
  if(!List.of("VERIFIED","REJECTED","PENDING").contains(decision)||note!=null&&note.length()>500||"REJECTED".equals(decision)&&(note==null||note.isBlank()))return "redirect:/admin/uploads?error=reviewNote";
  if("partner-doc".equals(kind)){
   UserAccount user=users.findById(ownerId).orElse(null);if(user==null)return "redirect:/admin/uploads?error";
   UploadedAsset asset=user.getDocuments().stream().filter(x->assetId.equals(x.getId())).findFirst().orElse(null);if(asset==null)return "redirect:/admin/uploads?error";
   asset.setVerificationStatus(decision);asset.setAdminNote(note==null?null:note.trim());
   if(!"VERIFIED".equals(decision)&&documentRequirements.mandatory(user.getRole()).contains(asset.getCategory())){user.setStatus("PENDING");user.setAvailable(false);}
   users.save(user);
  }else if("vehicle-doc".equals(kind)){
   Vehicle vehicle=vehicles.findById(ownerId).orElse(null);if(vehicle==null)return "redirect:/admin/uploads?error";
   UploadedAsset asset=vehicle.getDocuments().stream().filter(x->assetId.equals(x.getId())).findFirst().orElse(null);if(asset==null)return "redirect:/admin/uploads?error";
   asset.setVerificationStatus(decision);asset.setAdminNote(note==null?null:note.trim());
   if(!"VERIFIED".equals(decision)&&documentRequirements.mandatory("VEHICLE").contains(asset.getCategory()))vehicle.setStatus("PENDING");
   vehicles.save(vehicle);
  }else if("vehicle-photo".equals(kind)){
   Vehicle vehicle=vehicles.findById(ownerId).orElse(null);if(vehicle==null)return "redirect:/admin/uploads?error";
   UploadedAsset asset=vehicle.getPhotos().stream().filter(x->assetId.equals(x.getId())).findFirst().orElse(null);if(asset==null)return "redirect:/admin/uploads?error";
   asset.setVerificationStatus(decision);asset.setAdminNote(note==null?null:note.trim());
   if(!"VERIFIED".equals(decision))vehicle.setStatus("PENDING");
   vehicles.save(vehicle);
  }else return "redirect:/admin/uploads?error";
  return "redirect:/admin/uploads?updated";
 }
 @GetMapping("/files/{kind}/{ownerId}/{assetId}") public ResponseEntity<byte[]> file(Authentication a,@PathVariable String kind,@PathVariable String ownerId,@PathVariable String assetId){
  UserAccount viewer=me(a);UploadedAsset asset=find(kind,ownerId,assetId);
  if(asset==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  if("vehicle-photo".equals(kind)&&"CUSTOMER".equals(viewer.getRole())&&!"VERIFIED".equals(asset.getVerificationStatus()))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  if(!authorized(viewer,kind,ownerId))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  if(asset.getPublicId()!=null&&asset.getPublicId().startsWith("demo:")){
   String path=asset.getPublicId().substring(5);
   if(!path.matches("/demo-documents/[A-Za-z0-9._-]+"))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
   try{byte[] bytes=new ClassPathResource("static"+path).getContentAsByteArray();MediaType media=MediaType.APPLICATION_OCTET_STREAM;try{if(asset.getContentType()!=null)media=MediaType.parseMediaType(asset.getContentType());}catch(Exception ignored){}boolean inline=media.getType().equals("image");ContentDisposition disposition=ContentDisposition.builder(inline?"inline":"attachment").filename(asset.getOriginalFilename()==null?"demo-download":asset.getOriginalFilename()).build();return ResponseEntity.ok().contentType(media).contentLength(bytes.length).header(HttpHeaders.CONTENT_DISPOSITION,disposition.toString()).header(HttpHeaders.CACHE_CONTROL,"no-store, private").header("X-Content-Type-Options","nosniff").body(bytes);}catch(Exception ex){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Demo sample file not found");}
  }
  String url=cloud.deliveryUrl(asset);if(url==null||url.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cloudinary is not configured");
  try{
   HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
   HttpRequest request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
   HttpResponse<byte[]> response=client.send(request,HttpResponse.BodyHandlers.ofByteArray());
   if(response.statusCode()<200||response.statusCode()>=300)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Cloudinary could not deliver this file");
   MediaType media=MediaType.APPLICATION_OCTET_STREAM;try{if(asset.getContentType()!=null)media=MediaType.parseMediaType(asset.getContentType());}catch(Exception ignored){}
   boolean inline=media.getType().equals("image");
   ContentDisposition disposition=ContentDisposition.builder(inline?"inline":"attachment").filename(asset.getOriginalFilename()==null?"download":asset.getOriginalFilename()).build();
   return ResponseEntity.ok().contentType(media).contentLength(response.body().length).header(HttpHeaders.CONTENT_DISPOSITION,disposition.toString()).header(HttpHeaders.CACHE_CONTROL,"no-store, private").header("X-Content-Type-Options","nosniff").body(response.body());
  }catch(ResponseStatusException ex){throw ex;}catch(Exception ex){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Unable to retrieve the file from Cloudinary");}
 }
 private boolean authorized(UserAccount u,String kind,String ownerId){if("ADMIN".equals(u.getRole()))return true;return switch(kind){case "profile","partner-doc"->u.getId().equals(ownerId);case "vehicle-photo"->vehicles.findById(ownerId).map(v->u.getId().equals(v.getOwnerId())||("CUSTOMER".equals(u.getRole())&&"APPROVED".equals(v.getStatus()))).orElse(false);case "vehicle-doc"->vehicles.findById(ownerId).map(v->u.getId().equals(v.getOwnerId())).orElse(false);case "payment-proof"->payments.findById(ownerId).map(p->u.getId().equals(p.getCustomerId())||u.getId().equals(p.getDriverId())||u.getId().equals(p.getFleetOwnerId())).orElse(false);case "support"->tickets.findById(ownerId).map(t->u.getId().equals(t.getUserId())).orElse(false);case "safety"->safety.findById(ownerId).map(s->u.getId().equals(s.getUserId())).orElse(false);default->false;};}
 private UploadedAsset find(String kind,String ownerId,String assetId){return switch(kind){case "profile"->users.findById(ownerId).map(UserAccount::getProfilePhoto).filter(Objects::nonNull).filter(x->assetId.equals(x.getId())).orElse(null);case "partner-doc"->users.findById(ownerId).map(UserAccount::getDocuments).orElse(List.of()).stream().filter(x->assetId.equals(x.getId())).findFirst().orElse(null);case "vehicle-photo"->vehicles.findById(ownerId).map(Vehicle::getPhotos).orElse(List.of()).stream().filter(x->assetId.equals(x.getId())).findFirst().orElse(null);case "vehicle-doc"->vehicles.findById(ownerId).map(Vehicle::getDocuments).orElse(List.of()).stream().filter(x->assetId.equals(x.getId())).findFirst().orElse(null);case "payment-proof"->payments.findById(ownerId).map(PaymentRecord::getPaymentProof).filter(Objects::nonNull).filter(x->assetId.equals(x.getId())).orElse(null);case "support"->tickets.findById(ownerId).map(t->{UploadedAsset legacy=t.getAttachments().stream().filter(x->assetId.equals(x.getId())).findFirst().orElse(null);if(legacy!=null)return legacy;return t.getMessages().stream().filter(Objects::nonNull).flatMap(msg->msg.getAttachments().stream()).filter(x->assetId.equals(x.getId())).findFirst().orElse(null);}).orElse(null);case "safety"->safety.findById(ownerId).map(SafetyReport::getAttachments).orElse(List.of()).stream().filter(x->assetId.equals(x.getId())).findFirst().orElse(null);default->null;};}

}
