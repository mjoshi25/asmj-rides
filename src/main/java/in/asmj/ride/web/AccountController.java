package in.asmj.ride.web;

import in.asmj.ride.model.AccountRoleRequest;
import in.asmj.ride.model.UserAccount;
import in.asmj.ride.repo.ContactChangeRequestRepository;
import in.asmj.ride.repo.AccountRoleRequestRepository;
import in.asmj.ride.repo.UserAccountRepository;
import in.asmj.ride.service.CloudinaryUploadService;
import in.asmj.ride.service.DocumentRequirementService;
import in.asmj.ride.model.UploadedAsset;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.time.Instant;
import java.util.Set;

@Controller
public class AccountController {
    private static final Set<String> REQUESTABLE_ROLES=Set.of("CUSTOMER","DRIVER","FLEET_OWNER","TRAVEL_AGENT");
    private final UserAccountRepository users;
    private final AccountRoleRequestRepository requests;
    private final CloudinaryUploadService uploads;
    private final DocumentRequirementService documentRequirements;
    private final ContactChangeRequestRepository contactChanges;
    public AccountController(UserAccountRepository users, AccountRoleRequestRepository requests,
            CloudinaryUploadService uploads, DocumentRequirementService documentRequirements, ContactChangeRequestRepository contactChanges) {
        this.users=users; this.requests=requests; this.uploads=uploads; this.documentRequirements=documentRequirements; this.contactChanges=contactChanges;
    }

    private UserAccount current(Authentication authentication) { return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow(); }

    @GetMapping("/profile")
    public String profileAlias(Authentication authentication, Model model, HttpSession session) { return account(authentication, model, session); }

    @GetMapping("/account")
    public String account(Authentication authentication, Model model, HttpSession session) {
        UserAccount user=current(authentication);
        model.addAttribute("user",user);
        model.addAttribute("contactChangeRequests",contactChanges.findByUserIdOrderByRequestedAtDesc(user.getId()));
        model.addAttribute("contactChangePending",contactChanges.findFirstByUserIdAndStatusIn(user.getId(),List.of("PENDING_ADMIN_REVIEW","VERIFICATION_PENDING")).orElse(null));
        model.addAttribute("grantedRoles",user.getGrantedRoles());
        model.addAttribute("activeRole",session.getAttribute("ACTIVE_ROLE") == null ? user.getRole() : session.getAttribute("ACTIVE_ROLE"));
        List<AccountRoleRequest> roleRequests=requests.findByUserIdOrderByRequestedAtDesc(user.getId());
        model.addAttribute("roleRequests",roleRequests);
        model.addAttribute("availableRoles",REQUESTABLE_ROLES.stream().filter(r -> !user.hasRole(r)).toList());
        Map<String,List<in.asmj.ride.model.DocumentRequirementConfig.DocumentDefinition>> definitions=new java.util.LinkedHashMap<>();
        roleRequests.stream().filter(r -> "PENDING".equals(r.getStatus())).forEach(r -> definitions.put(r.getRequestedRole(),documentRequirements.definitions(r.getRequestedRole()).stream().filter(in.asmj.ride.model.DocumentRequirementConfig.DocumentDefinition::isActive).toList()));
        Map<String,List<in.asmj.ride.model.DocumentRequirementConfig.DocumentDefinition>> reuploadDefinitions=new java.util.LinkedHashMap<>();
        user.getRoleStatuses().forEach((role,status) -> { if ("PENDING".equals(status) && user.hasRole(role)) reuploadDefinitions.put(role,documentRequirements.definitions(role).stream().filter(in.asmj.ride.model.DocumentRequirementConfig.DocumentDefinition::isActive).toList()); });
        model.addAttribute("roleDocumentDefinitions",definitions);
        model.addAttribute("reuploadDefinitions",reuploadDefinitions);
        model.addAttribute("cloudinaryReady",uploads.configured());
        return "account";
    }

    @PostMapping("/account/roles/request")
    public String requestRole(Authentication authentication, @RequestParam String role,
            @RequestParam(required=false) String note) {
        UserAccount user=current(authentication);
        String normalized=role==null?"":role.trim().toUpperCase();
        if ("VEHICLE_OWNER".equals(normalized)) normalized="FLEET_OWNER";
        if (!REQUESTABLE_ROLES.contains(normalized) || user.hasRole(normalized)) return "redirect:/account?roleRequestError";
        if (requests.findFirstByUserIdAndRequestedRoleAndStatus(user.getId(),normalized,"PENDING").isPresent())
            return "redirect:/account?roleRequestPending";
        AccountRoleRequest request=new AccountRoleRequest();
        request.setUserId(user.getId()); request.setRequestedRole(normalized);
        request.setApplicantNote(note==null?null:note.trim()); request.setRequestedAt(Instant.now());
        requests.save(request);
        return "redirect:/account?roleRequestSubmitted";
    }


    @PostMapping("/account/roles/{role}/documents")
    public String uploadRoleDocument(Authentication authentication, @PathVariable String role,
            @RequestParam String documentType, @RequestParam(required=false) String expiryDate,
            @RequestParam MultipartFile file) {
        UserAccount user=current(authentication);
        String normalized=role==null?"":role.trim().toUpperCase();
        boolean pending=requests.findFirstByUserIdAndRequestedRoleAndStatus(user.getId(),normalized,"PENDING").isPresent();
        boolean reupload=user.hasRole(normalized) && "PENDING".equals(user.getRoleStatus(normalized));
        if ((!pending && !reupload) || !documentRequirements.allowedForUpload(normalized).contains(documentType)) return "redirect:/account?documentError";
        var definition=documentRequirements.definition(normalized,documentType);
        if (definition!=null && definition.isExpiryDateRequired() && (expiryDate==null || expiryDate.isBlank())) return "redirect:/account?expiryRequired";
        UploadedAsset uploaded=null;
        try {
            uploaded=uploads.upload(file,"asmj-ride/role-documents/"+user.getId()+"/"+normalized,documentType,true);
            uploaded.setExpiryDate(expiryDate);
            List<UploadedAsset> docs=user.getDocuments()==null?new ArrayList<>():new ArrayList<>(user.getDocuments());
            docs.add(uploaded); user.setDocuments(docs); users.save(user);
            return "redirect:/account?documentUploaded";
        } catch (RuntimeException ex) {
            if (uploaded!=null) try { uploads.delete(uploaded); } catch (RuntimeException ignored) { }
            return "redirect:/account?documentError";
        }
    }

    @PostMapping("/account/active-role")
    public String switchRole(Authentication authentication, @RequestParam String role, HttpSession session) {
        UserAccount user=current(authentication);
        String normalized=role==null?"":role.trim().toUpperCase();
        if (!user.getGrantedRoles().contains(normalized)) return "redirect:/account?roleSwitchError";
        session.setAttribute("ACTIVE_ROLE",normalized);
        return "redirect:/dashboard";
    }
}
