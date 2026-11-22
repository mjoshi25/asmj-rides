package in.asmj.ride.web;

import in.asmj.ride.model.AccountRoleRequest;
import in.asmj.ride.model.UserAccount;
import in.asmj.ride.repo.AccountRoleRequestRepository;
import in.asmj.ride.repo.UserAccountRepository;
import in.asmj.ride.service.DocumentRequirementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Set;

@Controller
public class AdminRoleRequestController {
    private static final Set<String> MANAGED_ROLES=Set.of("CUSTOMER","DRIVER","FLEET_OWNER","TRAVEL_AGENT");
    private final AccountRoleRequestRepository requests;
    private final UserAccountRepository users;
    private final DocumentRequirementService documentRequirements;
    public AdminRoleRequestController(AccountRoleRequestRepository requests, UserAccountRepository users,
            DocumentRequirementService documentRequirements) {
        this.requests=requests; this.users=users; this.documentRequirements=documentRequirements;
    }

    @GetMapping("/admin/role-requests")
    public String requests(Model model) {
        model.addAttribute("requests",requests.findByStatusOrderByRequestedAtAsc("PENDING"));
        model.addAttribute("users",users.findAll().stream().collect(java.util.stream.Collectors.toMap(UserAccount::getId,u->u,(a,b)->a)));
        return "admin-role-requests";
    }

    @PostMapping("/admin/role-requests/{id}/decision")
    public String decide(@PathVariable String id, @RequestParam String decision,
            @RequestParam(required=false) String adminNote, org.springframework.security.core.Authentication authentication) {
        AccountRoleRequest request=requests.findById(id).orElseThrow();
        if (!"PENDING".equals(request.getStatus()) || !MANAGED_ROLES.contains(request.getRequestedRole())) return "redirect:/admin/role-requests?error";
        UserAccount user=users.findById(request.getUserId()).orElse(null);
        if (user==null) return "redirect:/admin/role-requests?error";
        if ("APPROVE".equals(decision)) {
            if (user.hasRole(request.getRequestedRole())) return "redirect:/admin/role-requests?alreadyAssigned";
            if (!"CUSTOMER".equals(request.getRequestedRole()) && !documentRequirements.verifiedUserDocuments(user,request.getRequestedRole()))
                return "redirect:/admin/role-requests?documentsRequired";
            user.grantRole(request.getRequestedRole());
            users.save(user);
            request.setStatus("APPROVED");
        } else if ("REJECT".equals(decision)) request.setStatus("REJECTED");
        else return "redirect:/admin/role-requests?error";
        request.setAdminNote(adminNote==null?null:adminNote.trim());
        request.setDecidedBy(authentication.getName()); request.setDecidedAt(Instant.now());
        requests.save(request);
        return "redirect:/admin/role-requests?updated";
    }
}
