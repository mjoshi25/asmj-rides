package in.asmj.ride.web;

import in.asmj.ride.model.*;
import in.asmj.ride.repo.*;
import in.asmj.ride.service.CloudinaryUploadService;
import in.asmj.ride.service.DocumentRequirementService;
import in.asmj.ride.service.AgentVehicleAuthorizationService;
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
    private final CloudinaryUploadService uploads; private final ServiceCityRepository cities; private final DocumentRequirementService documentRequirements; private final TravelAgentProfileRepository agentProfiles; private final AgentVehicleAuthorizationService agentAuthorizations;
    public PortalController(UserAccountRepository u,VehicleRepository v,CloudinaryUploadService c,ServiceCityRepository cities,DocumentRequirementService documentRequirements,TravelAgentProfileRepository agentProfiles,AgentVehicleAuthorizationService agentAuthorizations){users=u;vehicles=v;uploads=c;this.cities=cities;this.documentRequirements=documentRequirements;this.agentProfiles=agentProfiles;this.agentAuthorizations=agentAuthorizations;}
    private UserAccount me(Authentication a){return users.findByEmailIgnoreCase(a.getName()).orElseThrow();}
    private void owner(UserAccount u,Vehicle v){
        if(u.getId().equals(v.getOwnerId())) return;
        if(u.getGrantedRoles().contains("TRAVEL_AGENT") && agentAuthorizations.canManage(u.getId(),v.getId(),"MANAGE_LISTING")) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @GetMapping("/partner") String partner(Authentication a,Model m){var u=me(a);m.addAttribute("name",u.getName());m.addAttribute("role",u.getGrantedRoles().contains("TRAVEL_AGENT")?"TRAVEL_AGENT":(u.getGrantedRoles().contains("FLEET_OWNER")?"FLEET_OWNER":u.getRole()));m.addAttribute("user",u);m.addAttribute("agentProfile",agentProfiles.findByUserId(u.getId()).orElseGet(TravelAgentProfile::new));m.addAttribute("vehicles",vehicles.findByOwnerId(u.getId()));m.addAttribute("pendingFleetOwner",u.getPendingFleetOwnerId()==null?null:users.findById(u.getPendingFleetOwnerId()).orElse(null));m.addAttribute("cities",cities.findByActiveTrueOrderByCityAsc());m.addAttribute("cloudinaryReady",uploads.configured());m.addAttribute("userDocumentDefinitions",documentRequirements.definitions(u.getGrantedRoles().contains("TRAVEL_AGENT")?"TRAVEL_AGENT":(u.getGrantedRoles().contains("FLEET_OWNER")?"FLEET_OWNER":u.getRole())).stream().filter(DocumentRequirementConfig.DocumentDefinition::isActive).toList());m.addAttribute("vehicleDocumentDefinitions",documentRequirements.definitions("VEHICLE").stream().filter(DocumentRequirementConfig.DocumentDefinition::isActive).toList());return "partner";}


    @GetMapping("/agent") String travelAgent(Authentication a,Model m){
        var u=me(a);
        m.addAttribute("name",u.getName());
        m.addAttribute("role","TRAVEL_AGENT");
        m.addAttribute("user",u);
        m.addAttribute("agentProfile",agentProfiles.findByUserId(u.getId()).orElseGet(TravelAgentProfile::new));
        m.addAttribute("vehicles",vehicles.findByOwnerId(u.getId()));
        m.addAttribute("pendingFleetOwner",null);
        m.addAttribute("cities",cities.findByActiveTrueOrderByCityAsc());
        m.addAttribute("cloudinaryReady",uploads.configured());
        m.addAttribute("userDocumentDefinitions",documentRequirements.definitions("TRAVEL_AGENT").stream().filter(DocumentRequirementConfig.DocumentDefinition::isActive).toList());
        m.addAttribute("vehicleDocumentDefinitions",documentRequirements.definitions("VEHICLE").stream().filter(DocumentRequirementConfig.DocumentDefinition::isActive).toList());
        return "partner";
    }


    @PostMapping("/agent/profile") String saveAgentProfile(Authentication a,
            @RequestParam String businessName,
            @RequestParam(required=false) String businessRegistrationNumber,
            @RequestParam(required=false) String businessAddress,
            @RequestParam(defaultValue="false") boolean ownFleetEnabled,
            @RequestParam(defaultValue="false") boolean partnerFleetEnabled) {
        UserAccount user=me(a);
        if(!user.hasGrantedRole("TRAVEL_AGENT") || businessName==null || businessName.isBlank()
                || businessName.trim().length()>120 || (!ownFleetEnabled && !partnerFleetEnabled))
            return "redirect:/agent?profileError";
        TravelAgentProfile profile=agentProfiles.findByUserId(user.getId()).orElseGet(TravelAgentProfile::new);
        profile.setUserId(user.getId());
        profile.setBusinessName(businessName.trim());
        profile.setBusinessRegistrationNumber(businessRegistrationNumber==null?null:businessRegistrationNumber.trim());
        profile.setBusinessAddress(businessAddress==null?null:businessAddress.trim());
        profile.setOwnFleetEnabled(ownFleetEnabled);
        profile.setPartnerFleetEnabled(partnerFleetEnabled);
        profile.setVerificationStatus("PENDING");
        profile.setUpdatedAt(java.time.Instant.now());
        agentProfiles.save(profile);
        return "redirect:/agent?profileSaved";
    }

    @PostMapping("/partner/fleet-association/respond")
    String respondFleetAssociation(Authentication a,@RequestParam String decision){
        UserAccount driver=me(a);
        if(!driver.hasGrantedRole("DRIVER") || driver.getPendingFleetOwnerId()==null || !"PENDING".equals(driver.getFleetAssociationStatus())) return "redirect:/partner?error=association";
        if("ACCEPT".equals(decision)){
            if(driver.getFleetOwnerId()!=null && !driver.getFleetOwnerId().isBlank()) return "redirect:/partner?error=association";
            driver.setFleetOwnerId(driver.getPendingFleetOwnerId()); driver.setFleetAssociationStatus("ACCEPTED");
        } else if("DECLINE".equals(decision)){ driver.setFleetAssociationStatus("DECLINED"); }
        else return "redirect:/partner?error=association";
        driver.setPendingFleetOwnerId(null); users.save(driver); return "redirect:/partner?associationUpdated";
    }

    @PostMapping("/profile/photo") String profilePhoto(Authentication a,@RequestParam MultipartFile photo){var u=me(a);try{var old=u.getProfilePhoto();var next=uploads.upload(photo,"asmj-ride/profile-photos","PROFILE_PHOTO",false);u.setProfilePhoto(next);users.save(u);safeDelete(old);return "redirect:/account?photoUpdated";}catch(RuntimeException ex){return redirectFor(u,"?uploadError="+enc(ex.getMessage()));}}

    @PostMapping("/partner/documents") String partnerDocument(Authentication a,@RequestParam String documentType,@RequestParam(required=false)String expiryDate,@RequestParam MultipartFile file){var u=me(a);String documentRole=u.getGrantedRoles().contains("TRAVEL_AGENT")?"TRAVEL_AGENT":(u.getGrantedRoles().contains("FLEET_OWNER")?"FLEET_OWNER":u.getRole());List<String> allowed=documentRequirements.allowedForUpload(documentRole);if(!allowed.contains(documentType))return "redirect:/partner?uploadError=invalidDocument";if(!validExpiry(documentRole,documentType,expiryDate))return "redirect:/partner?uploadError=expiryDateRequired";UploadedAsset asset=null;List<UploadedAsset> replaced=new ArrayList<>();try{asset=uploads.upload(file,"asmj-ride/partner-documents/"+u.getId(),documentType,true);asset.setExpiryDate(expiryDate);u.getDocuments().removeIf(x->{if(documentType.equals(x.getCategory())){replaced.add(x);return true;}return false;});u.getDocuments().add(asset);if(documentRequirements.mandatory(documentRole).contains(documentType)){
            if(documentRole.equals(u.getRole())) { u.setStatus("PENDING"); u.setAvailable(false); }
            else u.setRoleStatus(documentRole,"PENDING");
        }users.save(u);replaced.forEach(this::safeDelete);return "redirect:/partner?documentUploaded";}catch(RuntimeException ex){safeDelete(asset);return "redirect:/partner?uploadError="+enc(ex.getMessage());}}

    @PostMapping("/customer/documents") String customerDocument(Authentication a,@RequestParam String documentType,@RequestParam(required=false)String expiryDate,@RequestParam MultipartFile file){UserAccount u=me(a);if(!u.hasGrantedRole("CUSTOMER")||!documentRequirements.allowedForUpload("CUSTOMER").contains(documentType))return "redirect:/customer?uploadError=invalidDocument";if(!validExpiry("CUSTOMER",documentType,expiryDate))return "redirect:/customer?uploadError=expiryDateRequired";UploadedAsset asset=null;List<UploadedAsset> replaced=new ArrayList<>();try{asset=uploads.upload(file,"asmj-ride/customer-documents/"+u.getId(),documentType,true);asset.setExpiryDate(expiryDate);u.getDocuments().removeIf(x->{if(documentType.equals(x.getCategory())){replaced.add(x);return true;}return false;});u.getDocuments().add(asset);users.save(u);replaced.forEach(this::safeDelete);return "redirect:/customer?documentUploaded";}catch(RuntimeException ex){safeDelete(asset);return "redirect:/customer?uploadError="+enc(ex.getMessage());}}

    private List<String> validateLocationKeys(List<String> submitted){
        if(submitted==null||submitted.isEmpty()||submitted.size()>40)throw new IllegalArgumentException("Select at least one and no more than 40 service locations.");
        LinkedHashSet<String> clean=new LinkedHashSet<>();
        for(String key:submitted){String[] parts=key==null?new String[0]:key.split("::",2);if(parts.length!=2)throw new IllegalArgumentException("Invalid service location.");
            ServiceCity city=cities.findById(parts[0]).filter(ServiceCity::isActive).orElse(null);
            if(city==null||!city.getPinCodes().contains(parts[1]))throw new IllegalArgumentException("A selected city/PIN is no longer active.");
            clean.add(parts[0]+"::"+parts[1]);
        }
        return new ArrayList<>(clean);
    }
    private List<String> validateRouteKeys(List<String> submitted,List<String> pickups,List<String> drops){
        if(submitted==null||submitted.isEmpty()||submitted.size()>100)throw new IllegalArgumentException("Select at least one permitted pickup-to-drop route.");
        Set<String> pickupCities=new HashSet<>(),dropCities=new HashSet<>();
        pickups.forEach(k->pickupCities.add(k.split("::",2)[0]));drops.forEach(k->dropCities.add(k.split("::",2)[0]));
        LinkedHashSet<String> clean=new LinkedHashSet<>();
        for(String key:submitted){String[] parts=key==null?new String[0]:key.split(">",2);if(parts.length!=2||!pickupCities.contains(parts[0])||!dropCities.contains(parts[1]))throw new IllegalArgumentException("A permitted route must connect one selected pickup city to one selected drop city.");clean.add(parts[0]+">"+parts[1]);}
        return new ArrayList<>(clean);
    }
    private void applyCoverage(Vehicle v,List<String> pickupInput,List<String> dropInput,List<String> routeInput,boolean oneWay,boolean roundTrip){
        List<String> pickups=validateLocationKeys(pickupInput),drops=validateLocationKeys(dropInput);
        if(!oneWay&&!roundTrip)throw new IllegalArgumentException("Enable at least one journey type.");
        v.setPickupLocationKeys(pickups);v.setDropLocationKeys(drops);v.setAllowedRouteKeys(validateRouteKeys(routeInput,pickups,drops));
        v.setSupportsOneWay(oneWay);v.setSupportsRoundTrip(roundTrip);
        String firstCityId=pickups.get(0).split("::",2)[0];ServiceCity first=cities.findById(firstCityId).orElseThrow();v.setBaseCityId(first.getId());v.setBaseCity(first.getCity());
    }

    @PostMapping("/partner/vehicle") String add(Authentication a,@RequestParam String category,@RequestParam String registrationNumber,@RequestParam(required=false)String model,@RequestParam(required=false)List<String> pickupLocations,@RequestParam(required=false)List<String> dropLocations,@RequestParam(required=false)List<String> allowedRouteKeys,@RequestParam(defaultValue="false")boolean supportsOneWay,@RequestParam(defaultValue="false")boolean supportsRoundTrip,@RequestParam("photos") List<MultipartFile> photos){
        var u=me(a);if(!(u.hasGrantedRole("DRIVER")||u.hasGrantedRole("FLEET_OWNER")||u.hasGrantedRole("TRAVEL_AGENT")))return "redirect:/partner";
        if(u.hasGrantedRole("TRAVEL_AGENT")){
            TravelAgentProfile profile=agentProfiles.findByUserId(u.getId()).orElse(null);
            if(profile==null || !profile.isOwnFleetEnabled() || !"APPROVED".equals(profile.getVerificationStatus()) || !"APPROVED".equals(u.getStatus())) return "redirect:/agent?agencyApprovalRequired";
        }
        if(!List.of("CAR","AUTO","BIKE","OTHER").contains(category)||registrationNumber==null||registrationNumber.isBlank()||registrationNumber.length()>20)return "redirect:/partner?error=vehicle";
        if(photos==null||photos.stream().filter(f->f!=null&&!f.isEmpty()).count()<1)return "redirect:/partner?uploadError=vehiclePhotoRequired";
        Vehicle v=new Vehicle();v.setOwnerId(u.getId());v.setCategory(category);v.setRegistrationNumber(registrationNumber.toUpperCase(Locale.ROOT));v.setModel(model);
        try{applyCoverage(v,pickupLocations,dropLocations,allowedRouteKeys,supportsOneWay,supportsRoundTrip);
            for(MultipartFile f:photos)if(f!=null&&!f.isEmpty()){if(v.getPhotos().size()>=8)throw new IllegalArgumentException("Maximum 8 vehicle photos allowed.");v.getPhotos().add(uploads.upload(f,"asmj-ride/vehicle-photos/"+u.getId()+"/"+registrationNumber.replaceAll("[^A-Za-z0-9-]",""),"VEHICLE_PHOTO",false));}
            vehicles.save(v);return "redirect:/partner?vehicleAdded";
        }catch(RuntimeException ex){v.getPhotos().forEach(this::safeDelete);return "redirect:/partner?uploadError="+enc(ex.getMessage());}
    }

    @PostMapping("/partner/vehicle/{id}/update") String updateVehicle(Authentication a,@PathVariable String id,@RequestParam String category,@RequestParam String registrationNumber,@RequestParam(required=false)String model,@RequestParam(required=false)List<String> pickupLocations,@RequestParam(required=false)List<String> dropLocations,@RequestParam(required=false)List<String> allowedRouteKeys,@RequestParam(defaultValue="false")boolean supportsOneWay,@RequestParam(defaultValue="false")boolean supportsRoundTrip,@RequestParam(defaultValue="ACTIVE")String operationalStatus,@RequestParam(value="photos",required=false)List<MultipartFile> photos,@RequestParam(value="removePhotoIds",required=false)List<String> removeIds){
        var u=me(a);Vehicle v=vehicles.findById(id).orElseThrow();owner(u,v);
        if(!List.of("CAR","AUTO","BIKE","OTHER").contains(category)||registrationNumber.isBlank()||registrationNumber.length()>20)return "redirect:/partner?error=vehicle";
        List<UploadedAsset> removed=new ArrayList<>(),added=new ArrayList<>();
        try{
            applyCoverage(v,pickupLocations,dropLocations,allowedRouteKeys,supportsOneWay,supportsRoundTrip);
            if(!List.of("ACTIVE","MAINTENANCE","BLOCKED").contains(operationalStatus))throw new IllegalArgumentException("Invalid operational status.");
            v.setOperationalStatus(operationalStatus);
            if(removeIds!=null)for(String rid:removeIds){var old=v.getPhotos().stream().filter(x->rid.equals(x.getId())).findFirst().orElse(null);if(old!=null){removed.add(old);v.getPhotos().remove(old);}}
            if(photos!=null)for(MultipartFile f:photos)if(f!=null&&!f.isEmpty()){if(v.getPhotos().size()>=8)throw new IllegalArgumentException("Maximum 8 vehicle photos allowed.");var asset=uploads.upload(f,"asmj-ride/vehicle-photos/"+v.getId(),"VEHICLE_PHOTO",false);v.getPhotos().add(asset);added.add(asset);}
            if(v.getPhotos().isEmpty())throw new IllegalArgumentException("At least one vehicle photo must remain.");
            v.setCategory(category);v.setRegistrationNumber(registrationNumber.toUpperCase(Locale.ROOT));v.setModel(model);v.setStatus("PENDING");vehicles.save(v);removed.forEach(this::safeDelete);return "redirect:/partner?vehicleUpdated";
        }catch(RuntimeException ex){added.forEach(this::safeDelete);return "redirect:/partner?uploadError="+enc(ex.getMessage());}
    }

    @PostMapping("/admin/vehicle/{id}/coverage") String adminVehicleCoverage(@PathVariable String id,@RequestParam(required=false)List<String> pickupLocations,@RequestParam(required=false)List<String> dropLocations,@RequestParam(required=false)List<String> allowedRouteKeys,@RequestParam(defaultValue="false")boolean supportsOneWay,@RequestParam(defaultValue="false")boolean supportsRoundTrip,@RequestParam(defaultValue="ACTIVE")String operationalStatus){
        Vehicle v=vehicles.findById(id).orElseThrow();
        try{applyCoverage(v,pickupLocations,dropLocations,allowedRouteKeys,supportsOneWay,supportsRoundTrip);if(!List.of("ACTIVE","MAINTENANCE","BLOCKED").contains(operationalStatus))throw new IllegalArgumentException("Invalid operational status.");v.setOperationalStatus(operationalStatus);v.setStatus("PENDING");vehicles.save(v);return "redirect:/admin/uploads?coverageUpdated";}
        catch(RuntimeException ex){return "redirect:/admin/uploads?coverageError";}
    }

    @PostMapping("/partner/vehicle/{id}/documents") String vehicleDocument(Authentication a,@PathVariable String id,@RequestParam String documentType,@RequestParam(required=false)String expiryDate,@RequestParam MultipartFile file){
        var u=me(a);Vehicle v=vehicles.findById(id).orElseThrow();owner(u,v);if(!documentRequirements.allowedForUpload("VEHICLE").contains(documentType))return "redirect:/partner?uploadError=invalidDocument";if(!validExpiry("VEHICLE",documentType,expiryDate))return "redirect:/partner?uploadError=expiryDateRequired";
        UploadedAsset asset=null;List<UploadedAsset> replaced=new ArrayList<>();try{asset=uploads.upload(file,"asmj-ride/vehicle-documents/"+v.getId(),documentType,true);asset.setExpiryDate(expiryDate);v.getDocuments().removeIf(x->{if(documentType.equals(x.getCategory())){replaced.add(x);return true;}return false;});v.getDocuments().add(asset);if(documentRequirements.mandatory("VEHICLE").contains(documentType))v.setStatus("PENDING");vehicles.save(v);replaced.forEach(this::safeDelete);return "redirect:/partner?vehicleDocumentUploaded";}catch(RuntimeException ex){safeDelete(asset);return "redirect:/partner?uploadError="+enc(ex.getMessage());}
    }

    @PostMapping("/partner/vehicle/{id}/assign")String assignDriver(Authentication a,@PathVariable String id,@RequestParam String driverEmail){var owner=me(a);if(!(owner.getGrantedRoles().contains("FLEET_OWNER")||owner.getGrantedRoles().contains("TRAVEL_AGENT")))return "redirect:/partner";if(owner.getGrantedRoles().contains("TRAVEL_AGENT")){TravelAgentProfile profile=agentProfiles.findByUserId(owner.getId()).orElse(null);if(profile==null||!profile.isOwnFleetEnabled()||!"APPROVED".equals(profile.getVerificationStatus())||!"APPROVED".equals(owner.getStatus()))return "redirect:/agent?agencyApprovalRequired";}var driver=users.findByEmailIgnoreCase(driverEmail).orElse(null);var v=vehicles.findById(id).orElseThrow();if(driver==null||!driver.hasGrantedRole("DRIVER")||!"APPROVED".equals(driver.getStatus())||!driver.isMobileVerified()||!owner.getId().equals(v.getOwnerId()))return "redirect:/partner?error=driver";v.setAssignedDriverId(driver.getId());vehicles.save(v);driver.setFleetOwnerId(owner.getId());users.save(driver);return "redirect:/partner?assigned";}

    @GetMapping("/admin")String admin(Model m){m.addAttribute("partners",users.findByRoleIn(List.of("DRIVER","FLEET_OWNER","TRAVEL_AGENT")));m.addAttribute("vehicles",vehicles.findAll());m.addAttribute("travelAgents",agentProfiles.findAll());return "admin";}
    @PostMapping("/admin/partner/{id}")String approve(@PathVariable String id,@RequestParam String status){var u=users.findById(id).orElseThrow();if(!List.of("APPROVED","REJECTED","PENDING","SUSPENDED").contains(status))throw new IllegalArgumentException();if(u.hasGrantedRole("TRAVEL_AGENT") && "APPROVED".equals(status))return "redirect:/admin?agentProfileApprovalRequired";if("APPROVED".equals(status)&&!documentRequirements.verifiedUserDocuments(u,u.hasGrantedRole("TRAVEL_AGENT")?"TRAVEL_AGENT":u.getRole()))return "redirect:/admin?documentsRequired";u.setStatus(status);users.save(u);return "redirect:/admin";}
    @PostMapping("/admin/agent-profile/{id}") String approveAgentProfile(@PathVariable String id,@RequestParam String status){
        TravelAgentProfile profile=agentProfiles.findById(id).orElseThrow();
        UserAccount user=users.findById(profile.getUserId()).orElseThrow();
        if(!user.hasGrantedRole("TRAVEL_AGENT") || !List.of("APPROVED","REJECTED","PENDING").contains(status)) return "redirect:/admin?agentProfileError";
        if("APPROVED".equals(status) && !documentRequirements.verifiedUserDocuments(user,"TRAVEL_AGENT")) return "redirect:/admin?documentsRequired";
        profile.setVerificationStatus(status);profile.setUpdatedAt(java.time.Instant.now());agentProfiles.save(profile);
        if("TRAVEL_AGENT".equals(user.getRole())) user.setStatus(status);
        else user.setRoleStatus("TRAVEL_AGENT",status);
        users.save(user);
        return "redirect:/admin?agentProfileUpdated";
    }
    @PostMapping("/admin/vehicle/{id}")String vehicle(@PathVariable String id,@RequestParam String status){var v=vehicles.findById(id).orElseThrow();if(!List.of("APPROVED","REJECTED","PENDING").contains(status))throw new IllegalArgumentException();if("APPROVED".equals(status)){if(!documentRequirements.verifiedVehicleDocuments(v))return "redirect:/admin/uploads?vehicleDocumentsRequired";if(v.getPickupLocationKeys()==null||v.getPickupLocationKeys().isEmpty()||v.getDropLocationKeys()==null||v.getDropLocationKeys().isEmpty()||v.getAllowedRouteKeys()==null||v.getAllowedRouteKeys().isEmpty())return "redirect:/admin/uploads?vehicleCoverageRequired";}v.setStatus(status);vehicles.save(v);return "redirect:/admin";}

    @GetMapping("/admin/document-requirements") String documentRequirements(Model model){model.addAttribute("catalog",documentRequirements.allDefinitions());model.addAttribute("auditLog",documentRequirements.auditLog());return "admin-document-requirements";}
    @PostMapping("/admin/document-requirements/add") String addDocument(@RequestParam String role,@RequestParam String code,@RequestParam String name,@RequestParam(required=false)String description,@RequestParam(defaultValue="false")boolean mandatory,@RequestParam(defaultValue="false")boolean expiryDateRequired,Authentication a){try{documentRequirements.add(role,code,name,description,mandatory,expiryDateRequired,a.getName());return "redirect:/admin/document-requirements?saved";}catch(IllegalArgumentException ex){return "redirect:/admin/document-requirements?error="+enc(ex.getMessage());}}
    @PostMapping("/admin/document-requirements/update") String updateDocument(@RequestParam String role,@RequestParam String code,@RequestParam String name,@RequestParam(required=false)String description,@RequestParam(defaultValue="false")boolean mandatory,@RequestParam(defaultValue="false")boolean expiryDateRequired,Authentication a){try{documentRequirements.update(role,code,name,description,mandatory,expiryDateRequired,a.getName());return "redirect:/admin/document-requirements?saved";}catch(IllegalArgumentException ex){return "redirect:/admin/document-requirements?error="+enc(ex.getMessage());}}
    @PostMapping("/admin/document-requirements/toggle") String toggleDocument(@RequestParam String role,@RequestParam String code,@RequestParam boolean active,Authentication a){try{documentRequirements.setActive(role,code,active,a.getName());return "redirect:/admin/document-requirements?saved";}catch(IllegalArgumentException ex){return "redirect:/admin/document-requirements?error="+enc(ex.getMessage());}}
    @PostMapping("/admin/document-requirements/delete") String deleteDocument(@RequestParam String role,@RequestParam String code,Authentication a){try{documentRequirements.delete(role,code,a.getName());return "redirect:/admin/document-requirements?saved";}catch(IllegalArgumentException ex){return "redirect:/admin/document-requirements?error="+enc(ex.getMessage());}}
    @PostMapping("/admin/document-requirements") String saveDocumentRequirements(@RequestParam String role,@RequestParam(required=false) List<String> requiredDocuments,Authentication authentication){try{documentRequirements.saveRole(role,requiredDocuments,authentication.getName());return "redirect:/admin/document-requirements?saved";}catch(IllegalArgumentException ex){return "redirect:/admin/document-requirements?error";}}

    private boolean validExpiry(String role,String code,String expiryDate){
        var definition=documentRequirements.definition(role,code);
        if(definition==null)return false;
        if(definition.isExpiryDateRequired()&&(expiryDate==null||expiryDate.isBlank()))return false;
        if(expiryDate!=null&&!expiryDate.isBlank())try{return java.time.LocalDate.parse(expiryDate).isAfter(java.time.LocalDate.now());}catch(java.time.DateTimeException ex){return false;}
        return true;
    }
    private String redirectFor(UserAccount u,String suffix){List<String> roles=u.getGrantedRoles();if(roles.contains("ADMIN"))return "redirect:/admin"+suffix;if(roles.contains("TRAVEL_AGENT"))return "redirect:/agent"+suffix;if(roles.contains("FLEET_OWNER")||roles.contains("DRIVER"))return "redirect:/partner"+suffix;return "redirect:/customer"+suffix;}
    private String enc(String s){return java.net.URLEncoder.encode(s==null?"Upload failed":s,java.nio.charset.StandardCharsets.UTF_8);}
    private void safeDelete(UploadedAsset asset){try{uploads.delete(asset);}catch(RuntimeException ignored){}}
}
