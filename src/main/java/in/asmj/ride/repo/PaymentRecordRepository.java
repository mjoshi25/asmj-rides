package in.asmj.ride.repo;
import in.asmj.ride.model.PaymentRecord;import org.springframework.data.mongodb.repository.MongoRepository;import java.util.*;
public interface PaymentRecordRepository extends MongoRepository<PaymentRecord,String>{Optional<PaymentRecord> findByRideId(String rideId);List<PaymentRecord> findAllByOrderByCreatedAtDesc();List<PaymentRecord> findByDriverIdOrderByCreatedAtDesc(String driverId);List<PaymentRecord> findByFleetOwnerIdOrderByCreatedAtDesc(String fleetOwnerId);}
