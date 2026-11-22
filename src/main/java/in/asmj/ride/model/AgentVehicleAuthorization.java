package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Explicit owner-granted authority for an agent to manage a partner-owned vehicle. */
@Document("agentVehicleAuthorizations")
@CompoundIndex(name="owner_agent_vehicle_unique", def="{'ownerId':1,'agentId':1,'vehicleId':1}", unique=true)
public class AgentVehicleAuthorization {
    @Id private String id;
    private String ownerId;
    private String agentId;
    private String vehicleId;
    private List<String> permissions = new ArrayList<>(List.of("VIEW_LISTING","MANAGE_ENQUIRIES"));
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, REVOKED
    private Instant requestedAt = Instant.now();
    private Instant decidedAt;
    private String decidedBy;

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getOwnerId(){return ownerId;} public void setOwnerId(String v){ownerId=v;}
    public String getAgentId(){return agentId;} public void setAgentId(String v){agentId=v;}
    public String getVehicleId(){return vehicleId;} public void setVehicleId(String v){vehicleId=v;}
    public List<String> getPermissions(){return permissions;} public void setPermissions(List<String> v){permissions=v==null?new ArrayList<>():new ArrayList<>(v);}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Instant getRequestedAt(){return requestedAt;} public void setRequestedAt(Instant v){requestedAt=v;}
    public Instant getDecidedAt(){return decidedAt;} public void setDecidedAt(Instant v){decidedAt=v;}
    public String getDecidedBy(){return decidedBy;} public void setDecidedBy(String v){decidedBy=v;}
}
