package in.asmj.ride.repo;

import in.asmj.ride.model.RideNotification;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RideNotificationRepository extends MongoRepository<RideNotification,String> {
    List<RideNotification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
}
