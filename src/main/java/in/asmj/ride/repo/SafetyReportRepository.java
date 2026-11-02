package in.asmj.ride.repo;
import in.asmj.ride.model.SafetyReport;import org.springframework.data.mongodb.repository.MongoRepository;import java.util.*;
public interface SafetyReportRepository extends MongoRepository<SafetyReport,String>{List<SafetyReport> findAllByOrderByCreatedAtDesc();}
