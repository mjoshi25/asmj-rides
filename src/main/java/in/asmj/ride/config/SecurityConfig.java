package in.asmj.ride.config;

import in.asmj.ride.repo.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.DisabledException;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder();}

    @Bean UserDetailsService details(UserAccountRepository repository){
        return email -> {
            var user=repository.findByEmailIgnoreCase(email).orElseThrow(()->new UsernameNotFoundException("Account not found"));
            
if (!"ADMIN".equals(user.getRole()) && !user.isMobileVerified()) {
    throw new DisabledException("Verify your mobile first");
}

if ("SUSPENDED".equals(user.getStatus())) {
    throw new DisabledException("Account suspended");
}
            return User.withUsername(user.getEmail()).password(user.getPasswordHash()).roles(user.getRole()).build();
        };
    }

    @Bean DaoAuthenticationProvider provider(UserDetailsService details,PasswordEncoder encoder){
        DaoAuthenticationProvider provider=new DaoAuthenticationProvider();
        provider.setUserDetailsService(details);provider.setPasswordEncoder(encoder);return provider;
    }

    @Bean SecurityFilterChain chain(HttpSecurity http,DaoAuthenticationProvider provider)throws Exception{
        http.authenticationProvider(provider).authorizeHttpRequests(auth->auth
            .requestMatchers("/","/register","/verify-mobile","/login","/access-denied","/css/**","/js/**","/images/**","/favicon.ico","/manifest.webmanifest","/actuator/health","/actuator/health/**").permitAll()
            .requestMatchers("/admin","/admin/**").hasRole("ADMIN")
            .requestMatchers("/files/**").hasAnyRole("CUSTOMER","DRIVER","FLEET_OWNER","ADMIN")
            .requestMatchers("/partner","/partner/**").hasAnyRole("DRIVER","FLEET_OWNER")
            .requestMatchers("/fleet","/fleet/**").hasRole("FLEET_OWNER")
            .requestMatchers("/admin/rides/**","/admin/operations/**","/admin/payments/**").hasRole("ADMIN")
            .requestMatchers("/driver","/driver/**").hasAnyRole("DRIVER","FLEET_OWNER")
            .requestMatchers("/customer","/customer/**","/rides/book","/rides/*/cancel").hasRole("CUSTOMER")
            .requestMatchers("/rides/*/accept","/rides/*/status","/rides/*/location","/rides/*/return/**","/rides/*/assignment/respond").hasAnyRole("DRIVER","FLEET_OWNER")
            .requestMatchers("/locations/**","/rides/estimate","/rides/available-vehicles").hasRole("CUSTOMER")
            .requestMatchers("/notifications","/notifications/**").hasAnyRole("CUSTOMER","DRIVER","FLEET_OWNER","ADMIN")
            .requestMatchers("/payments","/payments/**","/support","/support/**","/rides/*/review","/rides/*/sos","/rides/*/complaint").hasAnyRole("CUSTOMER","DRIVER","FLEET_OWNER","ADMIN")
            .anyRequest().authenticated())
            .formLogin(form->form.loginPage("/login").defaultSuccessUrl("/dashboard",true).failureUrl("/login?error").permitAll())
            .exceptionHandling(ex->ex.accessDeniedPage("/access-denied"))
            .sessionManagement(session->session.sessionFixation(fixation->fixation.migrateSession()))
            .logout(logout->logout.logoutSuccessUrl("/").permitAll());
        http.headers(headers->headers.contentTypeOptions(c->{}).frameOptions(f->f.deny())
            .referrerPolicy(r->r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            .httpStrictTransportSecurity(hsts->hsts.includeSubDomains(true).maxAgeInSeconds(31536000)));
        return http.build();
    }
}
