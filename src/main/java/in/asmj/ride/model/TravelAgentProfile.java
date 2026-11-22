package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document("travelAgentProfiles")
public class TravelAgentProfile {
    @Id private String id;
    private String userId;
    private String businessName;
    private String businessRegistrationNumber;
    private String businessAddress;
    private boolean ownFleetEnabled = true;
    private boolean partnerFleetEnabled = true;
    private String verificationStatus = "PENDING";
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
    public String getBusinessName(){return businessName;} public void setBusinessName(String v){businessName=v;}
    public String getBusinessRegistrationNumber(){return businessRegistrationNumber;} public void setBusinessRegistrationNumber(String v){businessRegistrationNumber=v;}
    public String getBusinessAddress(){return businessAddress;} public void setBusinessAddress(String v){businessAddress=v;}
    public boolean isOwnFleetEnabled(){return ownFleetEnabled;} public void setOwnFleetEnabled(boolean v){ownFleetEnabled=v;}
    public boolean isPartnerFleetEnabled(){return partnerFleetEnabled;} public void setPartnerFleetEnabled(boolean v){partnerFleetEnabled=v;}
    public String getVerificationStatus(){return verificationStatus;} public void setVerificationStatus(String v){verificationStatus=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
