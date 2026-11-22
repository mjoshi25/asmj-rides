package in.asmj.ride.web;

import in.asmj.ride.model.ContactChangeRequest;
import in.asmj.ride.model.UserAccount;
import in.asmj.ride.repo.ContactChangeRequestRepository;
import in.asmj.ride.repo.UserAccountRepository;
import in.asmj.ride.service.RideNotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
public class ContactChangeController {
    private static final List<String> OPEN_STATES=List.of("PENDING_ADMIN_REVIEW","VERIFICATION_PENDING");
    private static final int MAX_ATTEMPTS=5;
    private final UserAccountRepository users; private final ContactChangeRequestRepository requests;
    private final RideNotificationService notifications; private final PasswordEncoder encoder;
    private final SecureRandom random=new SecureRandom();
    public ContactChangeController(UserAccountRepository users, ContactChangeRequestRepository requests,
            RideNotificationService notifications, PasswordEncoder encoder){
        this.users=users;this.requests=requests;this.notifications=notifications;this.encoder=encoder;
    }
    private UserAccount me(Authentication a){return users.findByEmailIgnoreCase(a.getName()).orElseThrow();}

    @PostMapping("/account/contact-change")
    public String requestChange(Authentication auth,@RequestParam(required=false) String newEmail,
            @RequestParam(required=false) String newMobile, RedirectAttributes flash){
        UserAccount user=me(auth);
        if(!"APPROVED".equalsIgnoreCase(user.getStatus())) return "redirect:/account?contactChangeNotApproved";
        if(requests.findFirstByUserIdAndStatusIn(user.getId(),OPEN_STATES).isPresent()) return "redirect:/account?contactChangePending";
        String email=clean(newEmail), mobile=clean(newMobile);
        if(email==null) email=user.getEmail(); else email=email.toLowerCase(Locale.ROOT);
        if(mobile==null) mobile=user.getMobile();
        if(email.equalsIgnoreCase(nullToEmpty(user.getEmail())) && mobile.equals(nullToEmpty(user.getMobile()))) return "redirect:/account?contactChangeNoChange";
        if(!email.matches("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$") || mobile.length()<8 || mobile.length()>16 || !mobile.matches("[+0-9 -]+")) return "redirect:/account?contactChangeInvalid";
        var emailOwner=users.findByEmailIgnoreCase(email);
        if(emailOwner.isPresent()&&!emailOwner.get().getId().equals(user.getId())) return "redirect:/account?contactChangeInUse";
        var mobileOwner=users.findByMobile(mobile);
        if(mobileOwner.isPresent()&&!mobileOwner.get().getId().equals(user.getId())) return "redirect:/account?contactChangeInUse";
        ContactChangeRequest request=new ContactChangeRequest(); request.setUserId(user.getId()); request.setUserName(user.getName());
        request.setOldEmail(user.getEmail()); request.setOldMobile(user.getMobile()); request.setNewEmail(email); request.setNewMobile(mobile);
        request.setRequestedBy(auth.getName()); request.setRequestedAt(Instant.now()); requests.save(request);
        notifications.notifyAdmins("Contact details change requested",user.getName()+" requested an update to registered contact details. Review and issue one security code.","/admin/contact-changes",request.getId(),"CONTACT_CHANGE");
        return "redirect:/account?contactChangeSubmitted";
    }

    @PostMapping("/account/contact-change/{id}/verify")
    public String verify(Authentication auth,@PathVariable String id,@RequestParam String code,HttpSession session){
        UserAccount user=me(auth); ContactChangeRequest r=requests.findById(id).orElseThrow();
        if(!user.getId().equals(r.getUserId())||!"VERIFICATION_PENDING".equals(r.getStatus())) return "redirect:/account?contactVerifyError";
        if(r.getCodeExpiresAt()==null||Instant.now().isAfter(r.getCodeExpiresAt())){r.setStatus("EXPIRED");r.setSecurityCodeHash(null);requests.save(r);return "redirect:/account?contactCodeExpired";}
        if(r.getVerificationAttempts()>=MAX_ATTEMPTS){r.setStatus("LOCKED");r.setSecurityCodeHash(null);requests.save(r);return "redirect:/account?contactCodeLocked";}
        r.setVerificationAttempts(r.getVerificationAttempts()+1);
        if(code==null||!encoder.matches(code.trim(),r.getSecurityCodeHash())){if(r.getVerificationAttempts()>=MAX_ATTEMPTS){r.setStatus("LOCKED");r.setSecurityCodeHash(null);}requests.save(r);return "redirect:/account?contactCodeInvalid";}
        if(!r.getNewEmail().equalsIgnoreCase(user.getEmail()) && users.findByEmailIgnoreCase(r.getNewEmail()).filter(u->!u.getId().equals(user.getId())).isPresent()) return "redirect:/account?contactChangeInUse";
        if(!r.getNewMobile().equals(user.getMobile()) && users.findByMobile(r.getNewMobile()).filter(u->!u.getId().equals(user.getId())).isPresent()) return "redirect:/account?contactChangeInUse";
        user.setEmail(r.getNewEmail()); user.setMobile(r.getNewMobile()); user.setMobileVerified(true); users.save(user);
        r.setStatus("COMPLETED");r.setVerifiedBy(auth.getName());r.setVerifiedAt(Instant.now());r.setCompletedAt(Instant.now());r.setSecurityCodeHash(null);requests.save(r);
        notifications.notifyAdmins("Contact details updated",user.getName()+" completed contact verification.","/admin/contact-changes",r.getId(),"CONTACT_CHANGE");
        SecurityContextHolder.clearContext();
        session.invalidate();
        return "redirect:/login?contactUpdated";
    }

    @GetMapping("/admin/contact-changes")
    public String adminQueue(Model model){
        model.addAttribute("pendingRequests",requests.findByStatusOrderByRequestedAtAsc("PENDING_ADMIN_REVIEW"));
        model.addAttribute("verificationRequests",requests.findByStatusOrderByRequestedAtAsc("VERIFICATION_PENDING"));
        model.addAttribute("history",requests.findAll().stream().filter(r->!OPEN_STATES.contains(r.getStatus())).sorted((a,b)->b.getRequestedAt().compareTo(a.getRequestedAt())).limit(50).toList());
        return "admin-contact-changes";
    }

    @PostMapping("/admin/contact-changes/{id}/issue-code")
    public String issueCode(Authentication auth,@PathVariable String id,RedirectAttributes flash){
        ContactChangeRequest r=requests.findById(id).orElseThrow();
        if(!"PENDING_ADMIN_REVIEW".equals(r.getStatus())) return "redirect:/admin/contact-changes?error";
        UserAccount user=users.findById(r.getUserId()).orElse(null); if(user==null||!"APPROVED".equalsIgnoreCase(user.getStatus())) return "redirect:/admin/contact-changes?error";
        String code=String.format("%06d",random.nextInt(1_000_000)); Instant now=Instant.now();
        r.setSecurityCodeHash(encoder.encode(code));r.setCodeIssuedAt(now);r.setCodeExpiresAt(now.plus(10,ChronoUnit.MINUTES));
        r.setCodeIssuedBy(auth.getName());r.setReviewedBy(auth.getName());r.setReviewedAt(now);r.setVerificationAttempts(0);r.setStatus("VERIFICATION_PENDING");requests.save(r);
        // The application has no configured SMS/email gateway. Display this one-time code to Admin for secure manual delivery to BOTH new channels.
        flash.addFlashAttribute("issuedCode",code);flash.addFlashAttribute("issuedFor",r.getNewMobile()+" / "+r.getNewEmail());
        return "redirect:/admin/contact-changes?codeIssued";
    }

    @PostMapping("/admin/contact-changes/{id}/reject")
    public String reject(Authentication auth,@PathVariable String id,@RequestParam(required=false) String note){
        ContactChangeRequest r=requests.findById(id).orElseThrow(); if(!"PENDING_ADMIN_REVIEW".equals(r.getStatus())) return "redirect:/admin/contact-changes?error";
        r.setStatus("REJECTED");r.setReviewedBy(auth.getName());r.setReviewedAt(Instant.now());r.setAdminNote(note);requests.save(r);
        notifications.notifyUser(r.getUserId(),"Contact change request declined","Your request to change account contact details was declined. Please contact support.","/support",null,"CONTACT_CHANGE");
        return "redirect:/admin/contact-changes?updated";
    }
    private static String clean(String s){return s==null||s.isBlank()?null:s.trim();}
    private static String nullToEmpty(String s){return s==null?"":s;}
}
