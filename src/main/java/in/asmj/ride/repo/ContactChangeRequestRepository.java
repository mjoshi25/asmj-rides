package in.asmj.ride.repo;
import in.asmj.ride.model.ContactChangeRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;
public interface ContactChangeRequestRepository extends MongoRepository<ContactChangeRequest,String> {
    List<ContactChangeRequest> findByStatusOrderByRequestedAtAsc(String status);
    List<ContactChangeRequest> findByUserIdOrderByRequestedAtDesc(String userId);
    Optional<ContactChangeRequest> findFirstByUserIdAndStatusIn(String userId, List<String> statuses);
}
