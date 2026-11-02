package in.asmj.ride.config;
import in.asmj.ride.repo.UserAccountRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
@Configuration public class SecurityConfig {
 @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService details(UserAccountRepository r){return email->{var u=r.findByEmailIgnoreCase(email).orElseThrow(()->new UsernameNotFoundException("Account not found"));if(!u.isMobileVerified())throw new DisabledException("Verify your mobile first");if((u.getRole().equals("DRIVER")||u.getRole().equals("FLEET_OWNER"))&&!u.getStatus().equals("APPROVED"))throw new DisabledException("Awaiting admin approval");if("SUSPENDED".equals(u.getStatus()))throw new DisabledException("Account suspended");return User.withUsername(u.getEmail()).password(u.getPasswordHash()).roles(u.getRole()).build();};}
 @Bean DaoAuthenticationProvider provider(UserDetailsService d,PasswordEncoder e){DaoAuthenticationProvider p=new DaoAuthenticationProvider();p.setUserDetailsService(d);p.setPasswordEncoder(e);return p;}
 @Bean SecurityFilterChain chain(HttpSecurity h,DaoAuthenticationProvider p)throws Exception{
  h.authenticationProvider(p).authorizeHttpRequests(a->a.requestMatchers("/","/register","/otp/**","/login","/css/**","/actuator/health","/actuator/health/**").permitAll()
   .requestMatchers("/admin","/admin/**").hasRole("ADMIN")
   .requestMatchers("/partner","/partner/**").hasAnyRole("DRIVER","FLEET_OWNER")
   .requestMatchers("/driver","/driver/**").hasRole("DRIVER")
   .requestMatchers("/customer","/customer/**","/rides/book","/rides/*/cancel").hasRole("CUSTOMER")
   .requestMatchers("/rides/*/accept","/rides/*/status","/rides/*/location").hasRole("DRIVER")
   .requestMatchers("/payments","/payments/**","/support","/support/**","/rides/*/review","/rides/*/sos","/rides/*/complaint").hasAnyRole("CUSTOMER","DRIVER","FLEET_OWNER","ADMIN")
   .anyRequest().authenticated())
   .formLogin(f->f.loginPage("/login").defaultSuccessUrl("/dashboard",true).permitAll())
   .logout(l->l.logoutSuccessUrl("/").permitAll());
  h.headers(headers -> headers
   .contentTypeOptions(c -> {})
   .frameOptions(f -> f.deny())
   .referrerPolicy(r -> r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
   .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)));
  return h.build();
 }
}
