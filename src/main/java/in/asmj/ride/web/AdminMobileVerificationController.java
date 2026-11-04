package in.asmj.ride.web;

import in.asmj.ride.model.UserAccount;
import in.asmj.ride.repo.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Controller
public class AdminMobileVerificationController {
    private static final Duration CODE_LIFETIME = Duration.ofHours(24);
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public AdminMobileVerificationController(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/admin/mobile-verifications")
    public String mobileVerifications(Model model) {
        List<UserAccount> accounts = users.findAll();
        accounts.sort(Comparator.comparing(UserAccount::getCreatedAt,
                Comparator.nullsLast(Comparator.<Instant>reverseOrder())));
        model.addAttribute("accounts", accounts);
        model.addAttribute("pendingCount", accounts.stream()
                .filter(u -> !"ADMIN".equals(u.getRole()) && !u.isMobileVerified()).count());
        return "admin-mobile-verifications";
    }

    @PostMapping("/admin/mobile-verifications/{id}/issue")
    public String issueCode(@PathVariable String id, Authentication authentication, RedirectAttributes redirect) {
        UserAccount user = users.findById(id).orElse(null);
        if (user == null) {
            redirect.addFlashAttribute("error", "Account not found.");
            return "redirect:/admin/mobile-verifications";
        }
        if ("ADMIN".equals(user.getRole()) || user.isMobileVerified()) {
            redirect.addFlashAttribute("error", "A security code cannot be issued for this account.");
            return "redirect:/admin/mobile-verifications";
        }
        if ("SUSPENDED".equals(user.getStatus()) || "REJECTED".equals(user.getStatus())) {
            redirect.addFlashAttribute("error", "This account is suspended or rejected. Resolve its account status first.");
            return "redirect:/admin/mobile-verifications";
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        Instant now = Instant.now();
        Instant expiresAt = now.plus(CODE_LIFETIME);
        user.setManualMobileCodeHash(passwordEncoder.encode(code));
        user.setManualMobileCodeIssuedAt(now);
        user.setManualMobileCodeExpiresAt(expiresAt);
        user.setManualMobileCodeIssuedBy(authentication == null ? "ADMIN" : authentication.getName());
        user.setManualMobileCodeAttempts(0);
        users.save(user);

        // The plain code is sent to the admin only once via a one-time flash message.
        // It is never persisted in MongoDB or written to application logs.
        redirect.addFlashAttribute("issuedCode", code);
        redirect.addFlashAttribute("issuedName", user.getName());
        redirect.addFlashAttribute("issuedEmail", user.getEmail());
        redirect.addFlashAttribute("issuedMobile", user.getMobile());
        redirect.addFlashAttribute("issuedExpiry", expiresAt.toString());
        redirect.addFlashAttribute("success", "Security code generated. Share it with the user manually using your verified contact channel.");
        return "redirect:/admin/mobile-verifications";
    }
}
