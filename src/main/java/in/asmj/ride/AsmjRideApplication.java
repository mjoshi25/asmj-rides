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
                System.out.println("Admin bootstrap skipped: ADMIN_USERNAME not configured.");
                return;
            }

            if (password == null || password.length() < 12) {
                throw new IllegalStateException(
                    "ADMIN_PASSWORD must contain at least 12 characters.");
            }

            if (!username.contains("@")) {
                throw new IllegalStateException("ADMIN_USERNAME must be a valid email.");
            }

            String email = username.trim().toLowerCase();

            var existing = repository.findByEmailIgnoreCase(email);
            UserAccount admin;
            if (existing.isPresent()) {
                admin = existing.get();
                if (!"ADMIN".equals(admin.getRole())) {
                    throw new IllegalStateException("ADMIN_USERNAME belongs to a non-admin account. Use a separate administrator email.");
                }
                // Keep the existing administrator password; do not reset it on every restart.
            } else {
                admin = new UserAccount();
                admin.setName("ASMJ Ride Administrator");
                admin.setEmail(email);
                admin.setMobile("0000000000");
                admin.setPasswordHash(encoder.encode(password));
                admin.setRole("ADMIN");
            }
            admin.setStatus("APPROVED");
            admin.setMobileVerified(true);
            admin.setAvailable(true);
            repository.save(admin);

            System.out.println("ASMJ Ride administrator bootstrap completed.");
        };
    }
}