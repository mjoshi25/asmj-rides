package in.asmj.ride.repo;
import in.asmj.ride.model.RideBooking;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface RideBookingRepository extends MongoRepository<RideBooking,String> {
 List<RideBooking> findByCustomerIdOrderByCreatedAtDesc(String customerId);
 List<RideBooking> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
 List<RideBooking> findByAssignedDriverIdOrderByCreatedAtDesc(String driverId);
}
