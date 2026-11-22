package in.asmj.ride.repo;

import in.asmj.ride.model.MobilityPlatformConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MobilityPlatformConfigRepository extends MongoRepository<MobilityPlatformConfig, String> { }
