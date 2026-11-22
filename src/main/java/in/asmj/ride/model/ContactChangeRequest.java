package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document("contact_change_requests")
public class ContactChangeRequest {
    @Id private String id;
    private String userId, userName, oldEmail, oldMobile, newEmail, newMobile;
    private String status = "PENDING_ADMIN_REVIEW";
    private String securityCodeHash, requestedBy, reviewedBy, codeIssuedBy, verifiedBy;
    private Instant requestedAt = Instant.now(), reviewedAt, codeIssuedAt, codeExpiresAt, verifiedAt, completedAt;
    private int verificationAttempts;
    private String adminNote;

    public String getId(){return id;} public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
    public String getUserName(){return userName;} public void setUserName(String v){userName=v;}
    public String getOldEmail(){return oldEmail;} public void setOldEmail(String v){oldEmail=v;}
    public String getOldMobile(){return oldMobile;} public void setOldMobile(String v){oldMobile=v;}
    public String getNewEmail(){return newEmail;} public void setNewEmail(String v){newEmail=v;}
    public String getNewMobile(){return newMobile;} public void setNewMobile(String v){newMobile=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getSecurityCodeHash(){return securityCodeHash;} public void setSecurityCodeHash(String v){securityCodeHash=v;}
    public String getRequestedBy(){return requestedBy;} public void setRequestedBy(String v){requestedBy=v;}
    public String getReviewedBy(){return reviewedBy;} public void setReviewedBy(String v){reviewedBy=v;}
    public String getCodeIssuedBy(){return codeIssuedBy;} public void setCodeIssuedBy(String v){codeIssuedBy=v;}
    public String getVerifiedBy(){return verifiedBy;} public void setVerifiedBy(String v){verifiedBy=v;}
    public Instant getRequestedAt(){return requestedAt;} public void setRequestedAt(Instant v){requestedAt=v;}
    public Instant getReviewedAt(){return reviewedAt;} public void setReviewedAt(Instant v){reviewedAt=v;}
    public Instant getCodeIssuedAt(){return codeIssuedAt;} public void setCodeIssuedAt(Instant v){codeIssuedAt=v;}
    public Instant getCodeExpiresAt(){return codeExpiresAt;} public void setCodeExpiresAt(Instant v){codeExpiresAt=v;}
    public Instant getVerifiedAt(){return verifiedAt;} public void setVerifiedAt(Instant v){verifiedAt=v;}
    public Instant getCompletedAt(){return completedAt;} public void setCompletedAt(Instant v){completedAt=v;}
    public int getVerificationAttempts(){return verificationAttempts;} public void setVerificationAttempts(int v){verificationAttempts=v;}
    public String getAdminNote(){return adminNote;} public void setAdminNote(String v){adminNote=v;}
}
