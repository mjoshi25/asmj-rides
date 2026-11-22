package in.asmj.ride.repo;

import in.asmj.ride.model.RentalPricingRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RentalPricingRuleRepository extends MongoRepository<RentalPricingRule,String> {
    List<RentalPricingRule> findByActiveTrue();
    List<RentalPricingRule> findByCategoryCodeAndActiveTrue(String categoryCode);
    List<RentalPricingRule> findByVehicleIdAndActiveTrue(String vehicleId);
}
