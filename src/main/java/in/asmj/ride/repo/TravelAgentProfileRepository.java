package in.asmj.ride.repo;

import in.asmj.ride.model.TravelAgentProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface TravelAgentProfileRepository extends MongoRepository<TravelAgentProfile,String> {
    Optional<TravelAgentProfile> findByUserId(String userId);
}
