package in.asmj.ride.repo;
import in.asmj.ride.model.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface VehicleRepository extends MongoRepository<Vehicle,String> {
 List<Vehicle> findByOwnerId(String ownerId);
 List<Vehicle> findByOwnerIdAndStatus(String ownerId,String status);
 List<Vehicle> findByStatus(String status);
}
