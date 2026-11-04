package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("users")
public class UserAccount {
    @Id private String id;
    private String name, email, mobile, passwordHash, role, status = "PENDING";
    private boolean mobileVerified, available;
    private String fleetOwnerId;
    private UploadedAsset profilePhoto;
    private List<UploadedAsset> documents = new ArrayList<>();
    private Instant createdAt = Instant.now();

    // Manual mobile-verification audit fields. Only a BCrypt hash of the code is stored.
    private String manualMobileCodeHash;
    private Instant manualMobileCodeIssuedAt;
    private Instant manualMobileCodeExpiresAt;
    private String manualMobileCodeIssuedBy;
    private int manualMobileCodeAttempts;
    private Instant mobileVerifiedAt;
    private String mobileVerificationMethod;

    // Legacy fields retained so existing MongoDB documents remain backward compatible.
    private String otpHash;
    private Instant otpExpiry, otpLastSentAt;

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getMobile() { return mobile; }
    public void setMobile(String value) { mobile = value; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { passwordHash = value; }
    public String getRole() { return role; }
    public void setRole(String value) { role = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean value) { available = value; }
    public String getFleetOwnerId() { return fleetOwnerId; }
    public void setFleetOwnerId(String value) { fleetOwnerId = value; }
    public boolean isMobileVerified() { return mobileVerified; }
    public void setMobileVerified(boolean value) { mobileVerified = value; }
    public UploadedAsset getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(UploadedAsset value) { profilePhoto = value; }
    public List<UploadedAsset> getDocuments() { return documents; }
    public void setDocuments(List<UploadedAsset> value) { documents = value == null ? new ArrayList<>() : value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }

    public String getManualMobileCodeHash() { return manualMobileCodeHash; }
    public void setManualMobileCodeHash(String value) { manualMobileCodeHash = value; }
    public Instant getManualMobileCodeIssuedAt() { return manualMobileCodeIssuedAt; }
    public void setManualMobileCodeIssuedAt(Instant value) { manualMobileCodeIssuedAt = value; }
    public Instant getManualMobileCodeExpiresAt() { return manualMobileCodeExpiresAt; }
    public void setManualMobileCodeExpiresAt(Instant value) { manualMobileCodeExpiresAt = value; }
    public String getManualMobileCodeIssuedBy() { return manualMobileCodeIssuedBy; }
    public void setManualMobileCodeIssuedBy(String value) { manualMobileCodeIssuedBy = value; }
    public int getManualMobileCodeAttempts() { return manualMobileCodeAttempts; }
    public void setManualMobileCodeAttempts(int value) { manualMobileCodeAttempts = value; }
    public Instant getMobileVerifiedAt() { return mobileVerifiedAt; }
    public void setMobileVerifiedAt(Instant value) { mobileVerifiedAt = value; }
    public String getMobileVerificationMethod() { return mobileVerificationMethod; }
    public void setMobileVerificationMethod(String value) { mobileVerificationMethod = value; }

    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String value) { otpHash = value; }
    public Instant getOtpExpiry() { return otpExpiry; }
    public void setOtpExpiry(Instant value) { otpExpiry = value; }
    public Instant getOtpLastSentAt() { return otpLastSentAt; }
    public void setOtpLastSentAt(Instant value) { otpLastSentAt = value; }
}
