package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/** Approval record for adding an additional capability to an existing account. */
@Document("accountRoleRequests")
public class AccountRoleRequest {
    @Id private String id;
    private String userId;
    private String requestedRole;
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, CANCELLED
    private String applicantNote;
    private String adminNote;
    private String decidedBy;
    private Instant requestedAt = Instant.now();
    private Instant decidedAt;

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
    public String getRequestedRole(){return requestedRole;} public void setRequestedRole(String v){requestedRole=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getApplicantNote(){return applicantNote;} public void setApplicantNote(String v){applicantNote=v;}
    public String getAdminNote(){return adminNote;} public void setAdminNote(String v){adminNote=v;}
    public String getDecidedBy(){return decidedBy;} public void setDecidedBy(String v){decidedBy=v;}
    public Instant getRequestedAt(){return requestedAt;} public void setRequestedAt(Instant v){requestedAt=v;}
    public Instant getDecidedAt(){return decidedAt;} public void setDecidedAt(Instant v){decidedAt=v;}
}
