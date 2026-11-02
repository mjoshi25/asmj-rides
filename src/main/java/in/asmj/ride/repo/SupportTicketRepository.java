package in.asmj.ride.repo;
import in.asmj.ride.model.SupportTicket;import org.springframework.data.mongodb.repository.MongoRepository;import java.util.*;
public interface SupportTicketRepository extends MongoRepository<SupportTicket,String>{List<SupportTicket> findByUserIdOrderByCreatedAtDesc(String userId);List<SupportTicket> findAllByOrderByCreatedAtDesc();}
