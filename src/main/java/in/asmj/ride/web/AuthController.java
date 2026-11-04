package in.asmj.ride.web;

import in.asmj.ride.model.UserAccount;
import in.asmj.ride.repo.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;

@Controller
public class AuthController {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    String home() { return "index"; }

    @GetMapping("/login")
    String login() { return "login"; }

    @GetMapping("/access-denied")
    String accessDenied() { return "access-denied"; }

    @GetMapping("/verify-mobile")
    String verifyMobilePage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email == null ? "" : email.trim());
        return "verify-mobile";
    }

    @PostMapping("/verify-mobile")
    String verifyMobile(@RequestParam String email, @RequestParam String securityCode, Model model) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String code = securityCode == null ? "" : securityCode.trim();
        model.addAttribute("email", normalizedEmail);

        if (!normalizedEmail.contains("@") || !code.matches("[0-9]{6}")) {
            model.addAttribute("error", "Enter your registered email and the 6-digit security code provided by ASMJ Ride support.");
            return "verify-mobile";
        }

        var account = repository.findByEmailIgnoreCase(normalizedEmail);
        if (account.isEmpty()) {
            model.addAttribute("error", "We could not verify these details. Check your email and contact ASMJ Ride support if needed.");
            return "verify-mobile";
        }

        UserAccount user = account.get();
        if ("ADMIN".equals(user.getRole())) {
            model.addAttribute("error", "Administrator accounts do not use customer mobile verification.");
            return "verify-mobile";
        }
        if (user.isMobileVerified()) {
            model.addAttribute("success", "Your mobile number is already verified. You can sign in.");
            return "login";
        }
        if ("SUSPENDED".equals(user.getStatus()) || "REJECTED".equals(user.getStatus())) {
            model.addAttribute("error", "This account is not eligible for mobile verification. Please contact ASMJ Ride support.");
            return "verify-mobile";
        }

        Instant now = Instant.now();
        if (user.getManualMobileCodeHash() == null || user.getManualMobileCodeExpiresAt() == null) {
            model.addAttribute("error", "No active security code is available. Please contact ASMJ Ride support and request a code.");
            return "verify-mobile";
        }
        if (now.isAfter(user.getManualMobileCodeExpiresAt())) {
            clearActiveCode(user);
            repository.save(user);
            model.addAttribute("error", "Your security code has expired. Please contact ASMJ Ride support to request a new code.");
            return "verify-mobile";
        }
        if (user.getManualMobileCodeAttempts() >= 5) {
            clearActiveCode(user);
            repository.save(user);
            model.addAttribute("error", "This security code has been locked after too many attempts. Please request a new code from support.");
            return "verify-mobile";
        }

        if (!passwordEncoder.matches(code, user.getManualMobileCodeHash())) {
            int attempts = user.getManualMobileCodeAttempts() + 1;
            user.setManualMobileCodeAttempts(attempts);
            if (attempts >= 5) clearActiveCode(user);
            repository.save(user);
            model.addAttribute("error", attempts >= 5
                    ? "Too many incorrect attempts. The code has been locked; please request a new code from support."
                    : "The security code is incorrect. Please check it and try again. Attempts remaining: " + (5 - attempts) + ".");
            return "verify-mobile";
        }

        user.setMobileVerified(true);
        user.setMobileVerifiedAt(now);
        user.setMobileVerificationMethod("ADMIN_ISSUED_SECURITY_CODE");
        clearActiveCode(user);
        repository.save(user);
        model.addAttribute("success", "Your mobile number has been verified successfully. You can now sign in.");
        return "login";
    }

    @GetMapping("/register")
    String register() { return "register"; }

    @PostMapping("/register")
    String register(@RequestParam String name, @RequestParam String email, @RequestParam String mobile,
                    @RequestParam String password, @RequestParam String role, Model model) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (name == null || name.isBlank() || mobile == null || !mobile.matches("[0-9]{10}")
                || password == null || password.length() < 8 || !normalizedEmail.contains("@")) {
            model.addAttribute("error", "Please enter valid details; password must be at least 8 characters.");
            return "register";
        }
        if (repository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            model.addAttribute("error", "Email already registered.");
            return "register";
        }
        if (role == null || !role.matches("CUSTOMER|DRIVER|FLEET_OWNER")) role = "CUSTOMER";

        UserAccount user = new UserAccount();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setMobile(mobile);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus("CUSTOMER".equals(role) ? "APPROVED" : "PENDING");
        user.setMobileVerified(false);
        repository.save(user);
        return "redirect:/verify-mobile?email=" + java.net.URLEncoder.encode(normalizedEmail, java.nio.charset.StandardCharsets.UTF_8);
    }

    @GetMapping("/dashboard")
    String dashboard(org.springframework.security.core.Authentication authentication) {
        var user = repository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return "redirect:" + (user.getRole().equals("ADMIN") ? "/admin"
                : user.getRole().equals("CUSTOMER") ? "/customer"
                : user.getRole().equals("DRIVER") ? "/driver" : "/partner");
    }

    private void clearActiveCode(UserAccount user) {
        user.setManualMobileCodeHash(null);
        user.setManualMobileCodeExpiresAt(null);
    }
}
