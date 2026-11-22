package in.asmj.ride.repo;
import in.asmj.ride.model.UserAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.*;
public interface UserAccountRepository extends MongoRepository<UserAccount,String> {
 Optional<UserAccount> findByEmailIgnoreCase(String email);
 @Query("{ '$or': [ { 'role': { '$in': ?0 } }, { 'roles': { '$in': ?0 } } ] }")
 List<UserAccount> findByRoleIn(List<String> roles);
 List<UserAccount> findByRoleAndStatusAndMobileVerified(String role,String status,boolean verified);
    Optional<UserAccount> findByMobile(String mobile);
}
