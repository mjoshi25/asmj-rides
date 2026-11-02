package in.asmj.ride;
import in.asmj.ride.model.UserAccount;import in.asmj.ride.repo.UserAccountRepository;
import org.springframework.boot.*;import org.springframework.boot.autoconfigure.SpringBootApplication;import org.springframework.context.annotation.Bean;import org.springframework.security.crypto.password.PasswordEncoder;
@SpringBootApplication public class AsmjRideApplication {
 public static void main(String[] args){SpringApplication.run(AsmjRideApplication.class,args);}
 @Bean CommandLineRunner seed(UserAccountRepository r,PasswordEncoder e){return a->{String n=System.getenv("ADMIN_USERNAME"),p=System.getenv("ADMIN_PASSWORD");if(n!=null&&!n.isBlank()&&p!=null&&p.length()>=12&&r.findByEmailIgnoreCase(n).isEmpty()){UserAccount u=new UserAccount();u.setName("ASMJ Ride Administrator");u.setEmail(n.toLowerCase());u.setMobile("0000000000");u.setPasswordHash(e.encode(p));u.setRole("ADMIN");u.setStatus("APPROVED");u.setMobileVerified(true);r.save(u);}};}
}