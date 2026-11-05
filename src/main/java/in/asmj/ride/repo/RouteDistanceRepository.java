package in.asmj.ride.repo;

import in.asmj.ride.model.RouteDistance;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RouteDistanceRepository extends MongoRepository<RouteDistance,String> {}
