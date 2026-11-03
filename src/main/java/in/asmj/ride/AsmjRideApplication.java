package in.asmj.ride;

import in.asmj.ride.model.UserAccount;
import in.asmj.ride.repo.UserAccountRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class AsmjRideApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsmjRideApplication.class, args);
    }

    @Bean
    CommandLineRunner seed(
            UserAccountRepository repository,
            PasswordEncoder encoder,
            @Value("${app.admin.username:}") String username,
            @Value("${app.admin.password:}") String password) {

        return args -> {
            if (username == null || username.isBlank()) {
                System.out.println("WARNING: Admin bootstrap skipped because ADMIN_USERNAME is not configured.");
                return;
            }
            if (password == null || password.length() < 12) {
                throw new IllegalStateException("ADMIN_PASSWORD must contain at least 12 characters.");
            }
            String email = username.trim().toLowerCase(java.util.Locale.ROOT);
            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new IllegalStateException("ADMIN_USERNAME must be a valid email address.");
            }

            var existing = repository.findByEmailIgnoreCase(email);
            if (existing.isPresent()) {
                UserAccount account = existing.get();
                if (!"ADMIN".equals(account.getRole())) {
                    throw new IllegalStateException(
                        "ADMIN_USERNAME already belongs to a non-admin account. Configure a different admin email.");
                }
                // Do not reset an existing administrator's password on every restart.
                if (!"APPROVED".equals(account.getStatus()) || !account.isMobileVerified()) {
                    account.setStatus("APPROVED");
                    account.setMobileVerified(true);
                    repository.save(account);
                }
                System.out.println("ASMJ Ride admin account already exists; existing password preserved.");
                return;
            }

            UserAccount admin = new UserAccount();
            admin.setName("ASMJ Ride Administrator");
            admin.setEmail(email);
            admin.setMobile("0000000000");
            admin.setPasswordHash(encoder.encode(password));
            admin.setRole("ADMIN");
            admin.setStatus("APPROVED");
            admin.setMobileVerified(true);
            admin.setAvailable(false);
            repository.save(admin);
            System.out.println("ASMJ Ride administrator created successfully for configured ADMIN_USERNAME.");
        };
    }

}