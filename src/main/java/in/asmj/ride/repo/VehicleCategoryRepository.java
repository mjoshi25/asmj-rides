package in.asmj.ride.repo;

import in.asmj.ride.model.VehicleCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface VehicleCategoryRepository extends MongoRepository<VehicleCategory,String> {
    Optional<VehicleCategory> findByCodeIgnoreCase(String code);
    List<VehicleCategory> findByActiveTrueOrderBySortOrderAscNameAsc();
}
