package in.asmj.ride.repo;
import in.asmj.ride.model.UserAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface UserAccountRepository extends MongoRepository<UserAccount,String> {
 Optional<UserAccount> findByEmailIgnoreCase(String email);
 List<UserAccount> findByRoleIn(List<String> roles);
 List<UserAccount> findByFleetOwnerIdAndRoleIn(String fleetOwnerId,List<String> roles);
 List<UserAccount> findByRoleAndStatusAndMobileVerified(String role,String status,boolean verified);
}
