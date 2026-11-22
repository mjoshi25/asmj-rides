package in.asmj.ride.repo;

import in.asmj.ride.model.RentalBooking;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RentalBookingRepository extends MongoRepository<RentalBooking,String> {
    List<RentalBooking> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<RentalBooking> findByVehicleIdAndStatusIn(String vehicleId,List<String> statuses);
    List<RentalBooking> findByVehicleOwnerIdOrderByCreatedAtDesc(String ownerId);
    List<RentalBooking> findByManagingAgentIdOrderByCreatedAtDesc(String agentId);
}
