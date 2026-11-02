package in.asmj.ride.web;

import in.asmj.ride.model.*;
import in.asmj.ride.repo.*;
import in.asmj.ride.service.CloudinaryUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Controller
public class PortalController {
    private final UserAccountRepository users; private final VehicleRepository vehicles;
    private final CloudinaryUploadService uploads;
    public PortalController(UserAccountRepository u,VehicleRepository v,CloudinaryUploadService c){users=u;vehicles=v;uploads=c;}
    private UserAccount me(Authentication a){return users.findByEmailIgnoreCase(a.getName()).orElseThrow();}
    private void owner(UserAccount u,Vehicle v){if(!u.getId().equals(v.getOwnerId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);}

    @GetMapping("/partner") String partner(Authentication a,Model m){var u=me(a);m.addAttribute("name",u.getName());m.addAttribute("role",u.getRole());m.addAttribute("user",u);m.addAttribute("vehicles",vehicles.findByOwnerId(u.getId()));m.addAttribute("cloudinaryReady",uploads.configured());return "partner";}

    @PostMapping("/profile/photo") String profilePhoto(Authentication a,@RequestParam MultipartFile photo){var u=me(a);try{var old=u.getProfilePhoto();var next=uploads.upload(photo,"asmj-ride/profile-photos","PROFILE_PHOTO",false);u.setProfilePhoto(next);users.save(u);safeDelete(old);return redirectFor(u,"?photoUpdated");}catch(RuntimeException ex){return redirectFor(u,"?uploadError="+enc(ex.getMessage()));}}

    @PostMapping("/partner/documents") String partnerDocument(Authentication a,@RequestParam String documentType,@RequestParam MultipartFile file){var u=me(a);List<String> allowed="DRIVER".equals(u.getRole())?List.of("IDENTITY","DRIVING_LICENCE","ADDRESS_PROOF"):List.of("IDENTITY","BUSINESS_REGISTRATION","ADDRESS_PROOF");if(!allowed.contains(documentType))return "redirect:/partner?uploadError=invalidDocument";UploadedAsset asset=null;List<UploadedAsset> replaced=new ArrayList<>();try{asset=uploads.upload(file,"asmj-ride/partner-documents/"+u.getId(),documentType,true);u.getDocuments().removeIf(x->{if(documentType.equals(x.getCategory())){replaced.add(x);return true;}return false;});u.getDocuments().add(asset);u.setStatus("PENDING");u.setAvailable(false);users.save(u);replaced.forEach(this::safeDelete);return "redirect:/partner?documentUploaded";}catch(RuntimeException ex){safeDelete(asset);return "redirect:/partner?uploadError="+enc(ex.getMessage());}}

    @PostMapping("/partner/vehicle") String add(Authentication a,@RequestParam String category,@RequestParam String registrationNumber,@RequestParam(required=false)String model,@RequestParam("photos") List<MultipartFile> photos){var u=me(a);if(!List.of("DRIVER","FLEET_OWNER").contains(u.getRole()))return "redirect:/partner";if(!List.of("CAR","AUTO","BIKE","OTHER").contains(category)||registrationNumber==null||registrationNumber.isBlank()||registrationNumber.length()>20)return "redirect:/partner?error=vehicle";if(photos==null||photos.stream().filter(f->f!=null&&!f.isEmpty()).count()<1)return "redirect:/partner?uploadError=vehiclePhotoRequired";Vehicle v=new Vehicle();v.setOwnerId(u.getId());v.setCategory(category);v.setRegistrationNumber(registrationNumber.toUpperCase(Locale.ROOT));v.setModel(model);try{for(MultipartFile f:photos)if(f!=null&&!f.isEmpty()){if(v.getPhotos().size()>=8)throw new IllegalArgumentException("Maximum 8 vehicle photos allowed.");v.getPhotos().add(uploads.upload(f,"asmj-ride/vehicle-photos/"+u.getId()+"/"+registrationNumber.replaceAll("[^A-Za-z0-9-]",""),"VEHICLE_PHOTO",false));}vehicles.save(v);return "redirect:/partner?vehicleAdded";}catch(RuntimeException ex){v.getPhotos().forEach(uploads::delete);return "redirect:/partner?uploadError="+enc(ex.getMessage());}}

    @PostMapping("/partner/vehicle/{id}/update") String updateVehicle(Authentication a,@PathVariable String id,@RequestParam String category,@RequestParam String registrationNumber,@RequestParam(required=false)String model,@RequestParam(value="photos",required=false)List<MultipartFile> photos,@RequestParam(value="removePhotoIds",required=false)List<String> removeIds){
        var u=me(a);Vehicle v=vehicles.findById(id).orElseThrow();owner(u,v);
        if(!List.of("CAR","AUTO","BIKE","OTHER").contains(category)||registrationNumber.isBlank()||registrationNumber.length()>20)return "redirect:/partner?error=vehicle";
        List<UploadedAsset> removed=new ArrayList<>(),added=new ArrayList<>();
        try{
            if(removeIds!=null)for(String rid:removeIds){var old=v.getPhotos().stream().filter(x->rid.equals(x.getId())).findFirst().orElse(null);if(old!=null){removed.add(old);v.getPhotos().remove(old);}}
            if(photos!=null)for(MultipartFile f:photos)if(f!=null&&!f.isEmpty()){if(v.getPhotos().size()>=8)throw new IllegalArgumentException("Maximum 8 vehicle photos allowed.");var asset=uploads.upload(f,"asmj-ride/vehicle-photos/"+v.getId(),"VEHICLE_PHOTO",false);v.getPhotos().add(asset);added.add(asset);}
            if(v.getPhotos().isEmpty())throw new IllegalArgumentException("At least one vehicle photo must remain.");
            v.setCategory(category);v.setRegistrationNumber(registrationNumber.toUpperCase(Locale.ROOT));v.setModel(model);v.setStatus("PENDING");vehicles.save(v);removed.forEach(this::safeDelete);return "redirect:/partner?vehicleUpdated";
        }catch(RuntimeException ex){added.forEach(this::safeDelete);return "redirect:/partner?uploadError="+enc(ex.getMessage());}
    }

    @PostMapping("/partner/vehicle/{id}/documents") String vehicleDocument(Authentication a,@PathVariable String id,@RequestParam String documentType,@RequestParam MultipartFile file){
        var u=me(a);Vehicle v=vehicles.findById(id).orElseThrow();owner(u,v);if(!List.of("RC","INSURANCE","PERMIT","FITNESS","PUC").contains(documentType))return "redirect:/partner?uploadError=invalidDocument";
        UploadedAsset asset=null;List<UploadedAsset> replaced=new ArrayList<>();try{asset=uploads.upload(file,"asmj-ride/vehicle-documents/"+v.getId(),documentType,true);v.getDocuments().removeIf(x->{if(documentType.equals(x.getCategory())){replaced.add(x);return true;}return false;});v.getDocuments().add(asset);v.setStatus("PENDING");vehicles.save(v);replaced.forEach(this::safeDelete);return "redirect:/partner?vehicleDocumentUploaded";}catch(RuntimeException ex){safeDelete(asset);return "redirect:/partner?uploadError="+enc(ex.getMessage());}
    }

    @PostMapping("/partner/vehicle/{id}/assign")String assignDriver(Authentication a,@PathVariable String id,@RequestParam String driverEmail){var owner=me(a);if(!"FLEET_OWNER".equals(owner.getRole()))return "redirect:/partner";var driver=users.findByEmailIgnoreCase(driverEmail).orElse(null);var v=vehicles.findById(id).orElseThrow();if(driver==null||!"DRIVER".equals(driver.getRole())||!"APPROVED".equals(driver.getStatus())||!driver.isMobileVerified()||!owner.getId().equals(v.getOwnerId()))return "redirect:/partner?error=driver";v.setAssignedDriverId(driver.getId());vehicles.save(v);driver.setFleetOwnerId(owner.getId());users.save(driver);return "redirect:/partner?assigned";}

    @GetMapping("/admin")String admin(Model m){m.addAttribute("partners",users.findByRoleIn(List.of("DRIVER","FLEET_OWNER")));m.addAttribute("vehicles",vehicles.findAll());return "admin";}
    @PostMapping("/admin/partner/{id}")String approve(@PathVariable String id,@RequestParam String status){var u=users.findById(id).orElseThrow();if(!List.of("APPROVED","REJECTED","PENDING","SUSPENDED").contains(status))throw new IllegalArgumentException();if("APPROVED".equals(status)){List<String> required="DRIVER".equals(u.getRole())?List.of("IDENTITY","DRIVING_LICENCE"):List.of("IDENTITY","BUSINESS_REGISTRATION");for(String type:required){var d=u.getDocuments().stream().filter(x->type.equals(x.getCategory())&&"VERIFIED".equals(x.getVerificationStatus())).findFirst().orElse(null);if(d==null)return "redirect:/admin?documentsRequired";}}u.setStatus(status);users.save(u);return "redirect:/admin";}
    @PostMapping("/admin/vehicle/{id}")String vehicle(@PathVariable String id,@RequestParam String status){var v=vehicles.findById(id).orElseThrow();if(!List.of("APPROVED","REJECTED","PENDING").contains(status))throw new IllegalArgumentException();if("APPROVED".equals(status)){if(v.getPhotos()==null||v.getPhotos().isEmpty())return "redirect:/admin?vehiclePhotoRequired";for(String type:List.of("RC","INSURANCE")){if(v.getDocuments().stream().noneMatch(x->type.equals(x.getCategory())&&"VERIFIED".equals(x.getVerificationStatus())))return "redirect:/admin?vehicleDocumentsRequired";}}v.setStatus(status);vehicles.save(v);return "redirect:/admin";}

    private String redirectFor(UserAccount u,String suffix){return switch(u.getRole()){case "DRIVER","FLEET_OWNER"->"redirect:/partner"+suffix;case "ADMIN"->"redirect:/admin"+suffix;default->"redirect:/customer"+suffix;};}
    private String enc(String s){return java.net.URLEncoder.encode(s==null?"Upload failed":s,java.nio.charset.StandardCharsets.UTF_8);}
    private void safeDelete(UploadedAsset asset){try{uploads.delete(asset);}catch(RuntimeException ignored){}}
}
