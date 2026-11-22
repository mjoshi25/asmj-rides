package in.asmj.ride.repo;

import in.asmj.ride.model.AccountRoleRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface AccountRoleRequestRepository extends MongoRepository<AccountRoleRequest,String> {
    List<AccountRoleRequest> findByUserIdOrderByRequestedAtDesc(String userId);
    List<AccountRoleRequest> findByStatusOrderByRequestedAtAsc(String status);
    Optional<AccountRoleRequest> findFirstByUserIdAndRequestedRoleAndStatus(String userId,String requestedRole,String status);
}
