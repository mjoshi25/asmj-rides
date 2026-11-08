package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.*;

@Document("document_requirement_config")
public class DocumentRequirementConfig {
    @Id private String id = "default";
    private Map<String, List<String>> mandatoryByRole = new LinkedHashMap<>();
    private List<DocumentDefinition> definitions = new ArrayList<>();
    private String updatedBy;
    private List<String> auditLog = new ArrayList<>();
    private Instant updatedAt = Instant.now();

    public String getId(){return id;} public void setId(String v){id=v;}
    public Map<String,List<String>> getMandatoryByRole(){return mandatoryByRole;}
    public void setMandatoryByRole(Map<String,List<String>> v){mandatoryByRole=v==null?new LinkedHashMap<>():v;}
    public List<DocumentDefinition> getDefinitions(){return definitions;}
    public void setDefinitions(List<DocumentDefinition> v){definitions=v==null?new ArrayList<>():v;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String v){updatedBy=v;}
    public List<String> getAuditLog(){return auditLog;} public void setAuditLog(List<String> v){auditLog=v==null?new ArrayList<>():v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}

    public static class DocumentDefinition {
        private String code; private String name; private String role; private String description;
        private boolean mandatory; private boolean active=true; private boolean expiryDateRequired;
        private boolean systemDefined;
        public DocumentDefinition(){}
        public DocumentDefinition(String code,String name,String role,boolean mandatory){this.code=code;this.name=name;this.role=role;this.mandatory=mandatory;this.systemDefined=true;}
        public String getCode(){return code;} public void setCode(String v){code=v;}
        public String getName(){return name;} public void setName(String v){name=v;}
        public String getRole(){return role;} public void setRole(String v){role=v;}
        public String getDescription(){return description;} public void setDescription(String v){description=v;}
        public boolean isMandatory(){return mandatory;} public void setMandatory(boolean v){mandatory=v;}
        public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
        public boolean isExpiryDateRequired(){return expiryDateRequired;} public void setExpiryDateRequired(boolean v){expiryDateRequired=v;}
        public boolean isSystemDefined(){return systemDefined;} public void setSystemDefined(boolean v){systemDefined=v;}
    }
}
