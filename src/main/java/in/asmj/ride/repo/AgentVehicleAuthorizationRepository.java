package in.asmj.ride.repo;

import in.asmj.ride.model.AgentVehicleAuthorization;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface AgentVehicleAuthorizationRepository extends MongoRepository<AgentVehicleAuthorization,String> {
    List<AgentVehicleAuthorization> findByAgentIdAndStatus(String agentId,String status);
    List<AgentVehicleAuthorization> findByOwnerIdAndStatus(String ownerId,String status);
    Optional<AgentVehicleAuthorization> findByOwnerIdAndAgentIdAndVehicleId(String ownerId,String agentId,String vehicleId);
}
