package in.asmj.ride.repo;
import in.asmj.ride.model.ServiceCity; import org.springframework.data.mongodb.repository.MongoRepository; import java.util.List;
public interface ServiceCityRepository extends MongoRepository<ServiceCity,String> { List<ServiceCity> findByActiveTrueOrderByCityAsc(); List<ServiceCity> findAllByOrderByCityAsc(); }
