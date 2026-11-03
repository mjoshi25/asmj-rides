package in.asmj.ride.web;

import in.asmj.ride.model.UserAccount;
import in.asmj.ride.repo.UserAccountRepository;
import in.asmj.ride.service.Msg91OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.SecureRandom;
import java.time.Instant;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class AuthController {
    private static final long OTP_EXPIRY_SECONDS = 300;
    private static final long RESEND_COOLDOWN_SECONDS = 30;

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Msg91OtpService msg91OtpService;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.otp.dev-mode:false}")
    private boolean devOtpMode;

    public AuthController(UserAccountRepository repository, PasswordEncoder passwordEncoder,
                          Msg91OtpService msg91OtpService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.msg91OtpService = msg91OtpService;
    }

    @GetMapping("/")
    String home() { return "index"; }

    @GetMapping("/login")
    String login() { return "login"; }

    @GetMapping("/access-denied")
    String accessDenied() { return "access-denied"; }

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
        return "redirect:/otp/request?email=" + URLEncoder.encode(normalizedEmail, StandardCharsets.UTF_8);
    }

    @GetMapping("/otp/request")
    String otp(@RequestParam String email, Model model) {
        var account = repository.findByEmailIgnoreCase(email.trim());
        if (account.isEmpty()) {
            model.addAttribute("error", "Account not found. Please register again.");
            model.addAttribute("email", email);
            return "otp";
        }
        if (account.get().isMobileVerified()) {
            model.addAttribute("success", "This mobile number is already verified. Please sign in.");
            model.addAttribute("email", email);
            return "login";
        }
        model.addAttribute("email", email);
        model.addAttribute("mobileHint", maskMobile(account.get().getMobile()));
        model.addAttribute("providerReady", msg91OtpService.isConfigured() || devOtpMode);
        return "otp";
    }

    @PostMapping("/otp/request")
    String sendOtp(@RequestParam String email, Model model) {
        var account = repository.findByEmailIgnoreCase(email.trim());
        if (account.isEmpty()) {
            model.addAttribute("error", "Account not found. Please register again.");
            model.addAttribute("email", email);
            return "otp";
        }
        UserAccount user = account.get();
        model.addAttribute("email", user.getEmail());
        model.addAttribute("mobileHint", maskMobile(user.getMobile()));

        if (user.isMobileVerified()) {
            model.addAttribute("success", "Your mobile number is already verified. Please sign in.");
            return "login";
        }
        if (user.getOtpLastSentAt() != null) {
            long secondsSinceLastSend = Instant.now().getEpochSecond() - user.getOtpLastSentAt().getEpochSecond();
            if (secondsSinceLastSend < RESEND_COOLDOWN_SECONDS) {
                model.addAttribute("error", "Please wait " + (RESEND_COOLDOWN_SECONDS - secondsSinceLastSend)
                        + " seconds before requesting another OTP.");
                return "otp";
            }
        }

        try {
            if (msg91OtpService.isConfigured()) {
                msg91OtpService.sendOtp(user.getMobile());
                user.setOtpHash(null);
                user.setOtpExpiry(Instant.now().plusSeconds(OTP_EXPIRY_SECONDS));
                model.addAttribute("success", "OTP sent to " + maskMobile(user.getMobile()) + ". It is valid for 5 minutes.");
            } else if (devOtpMode) {
                String code = String.format("%06d", random.nextInt(1_000_000));
                user.setOtpHash(passwordEncoder.encode(code));
                user.setOtpExpiry(Instant.now().plusSeconds(OTP_EXPIRY_SECONDS));
                model.addAttribute("devOtp", code);
                model.addAttribute("success", "Development OTP generated (5-minute expiry).");
            } else {
                model.addAttribute("error", "SMS verification is not configured. Please contact ASMJ Ride support.");
                model.addAttribute("providerReady", false);
                return "otp";
            }
            user.setOtpLastSentAt(Instant.now());
            repository.save(user);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        model.addAttribute("providerReady", msg91OtpService.isConfigured() || devOtpMode);
        return "otp";
    }

    @PostMapping("/otp/verify")
    String verifyOtp(@RequestParam String email, @RequestParam String otp, Model model) {
        var account = repository.findByEmailIgnoreCase(email.trim());
        if (account.isEmpty()) {
            model.addAttribute("error", "Account not found.");
            model.addAttribute("email", email);
            return "otp";
        }
        UserAccount user = account.get();
        model.addAttribute("email", user.getEmail());
        model.addAttribute("mobileHint", maskMobile(user.getMobile()));

        if (user.isMobileVerified()) {
            model.addAttribute("success", "Mobile already verified. Please sign in.");
            return "login";
        }
        if (user.getOtpExpiry() == null || Instant.now().isAfter(user.getOtpExpiry())) {
            model.addAttribute("error", "OTP has expired. Please request a new OTP.");
            return "otp";
        }

        boolean verified;
        try {
            if (msg91OtpService.isConfigured()) {
                verified = msg91OtpService.verifyOtp(user.getMobile(), otp);
            } else {
                verified = devOtpMode && user.getOtpHash() != null && otp != null
                        && passwordEncoder.matches(otp.trim(), user.getOtpHash());
            }
        } catch (IllegalStateException | IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "otp";
        }

        if (!verified) {
            model.addAttribute("error", "Invalid OTP. Please check the code and try again.");
            return "otp";
        }

        user.setMobileVerified(true);
        user.setOtpHash(null);
        user.setOtpExpiry(null);
        repository.save(user);
        model.addAttribute("success", "Mobile verified successfully. You can now sign in.");
        return "login";
    }

    @GetMapping("/dashboard")
    String dashboard(org.springframework.security.core.Authentication authentication) {
        var user = repository.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return "redirect:" + (user.getRole().equals("ADMIN") ? "/admin"
                : user.getRole().equals("CUSTOMER") ? "/customer"
                : user.getRole().equals("DRIVER") ? "/driver" : "/partner");
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "your registered mobile";
        return "******" + mobile.substring(mobile.length() - 4);
    }
}
