package in.asmj.ride.repo;
import in.asmj.ride.model.RideReview;import org.springframework.data.mongodb.repository.MongoRepository;import java.util.*;
public interface RideReviewRepository extends MongoRepository<RideReview,String>{Optional<RideReview> findByRideId(String rideId);List<RideReview> findAllByOrderByCreatedAtDesc();}
