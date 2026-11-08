package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.*;

@Document("document_requirement_config")
public class DocumentRequirementConfig {
    @Id private String id = "default";
    private Map<String, List<String>> mandatoryByRole = new LinkedHashMap<>();
    private String updatedBy;
    private Instant updatedAt = Instant.now();
    public String getId(){return id;} public void setId(String v){id=v;}
    public Map<String,List<String>> getMandatoryByRole(){return mandatoryByRole;}
    public void setMandatoryByRole(Map<String,List<String>> v){mandatoryByRole=v==null?new LinkedHashMap<>():v;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String v){updatedBy=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
