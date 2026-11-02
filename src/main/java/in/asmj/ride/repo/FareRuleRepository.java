package in.asmj.ride.repo;
import in.asmj.ride.model.FareRule;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface FareRuleRepository extends MongoRepository<FareRule,String> {}
