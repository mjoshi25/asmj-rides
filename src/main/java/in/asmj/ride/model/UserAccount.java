package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;

@Document("users")
public class UserAccount {
    @Id private String id;
    private String name, email, mobile, passwordHash, role, status = "PENDING";
    // New multi-role field. Legacy documents continue to use the primary `role` field.
    private List<String> roles = new ArrayList<>();
    // Additional roles have independent approval states; legacy primary role keeps legacy status semantics.
    private Map<String, String> roleStatuses = new LinkedHashMap<>();
    private boolean mobileVerified, available, fleetOwnerDriverEnabled;
    private String fleetOwnerId;
    private String pendingFleetOwnerId;
    private String fleetAssociationStatus;
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
    public void setRole(String value) {
        role = normalizeRole(value);
        if (roles == null) roles = new ArrayList<>();
        if (role != null && !roles.contains(role)) roles.add(role);
    }
    public List<String> getRoles() {
        LinkedHashSet<String> all = new LinkedHashSet<>();
        if (roles != null) roles.stream().filter(Objects::nonNull).map(UserAccount::normalizeRole).filter(Objects::nonNull).forEach(all::add);
        if (role != null && normalizeRole(role) != null) all.add(normalizeRole(role));
        return new ArrayList<>(all);
    }
    public void setRoles(List<String> values) {
        LinkedHashSet<String> clean = new LinkedHashSet<>();
        if (values != null) values.stream().filter(Objects::nonNull).map(UserAccount::normalizeRole).filter(Objects::nonNull).forEach(clean::add);
        if (role != null) clean.add(normalizeRole(role));
        roles = new ArrayList<>(clean);
    }
    public boolean hasRole(String value) { return value != null && getRoles().contains(normalizeRole(value)); }
    public boolean hasGrantedRole(String value) { return value != null && getGrantedRoles().contains(normalizeRole(value)); }
    public Map<String,String> getRoleStatuses() { return roleStatuses == null ? new LinkedHashMap<>() : new LinkedHashMap<>(roleStatuses); }
    public void setRoleStatuses(Map<String,String> values) { roleStatuses = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values); }
    public void setRoleStatus(String roleName, String statusValue) {
        String normalized = normalizeRole(roleName);
        if (normalized == null) return;
        if (roleStatuses == null) roleStatuses = new LinkedHashMap<>();
        roleStatuses.put(normalized, statusValue);
    }
    public String getRoleStatus(String roleName) {
        String normalized = normalizeRole(roleName);
        if (normalized == null) return null;
        if (roleStatuses != null && roleStatuses.containsKey(normalized)) return roleStatuses.get(normalized);
        return normalized.equals(normalizeRole(role)) ? status : null;
    }
    public List<String> getGrantedRoles() {
        List<String> granted = new ArrayList<>();
        for (String candidate : getRoles()) {
            // Preserve legacy primary-role access; additional roles are granted only after Admin approval.
            if (candidate.equals(normalizeRole(role)) || "APPROVED".equals(getRoleStatus(candidate))) granted.add(candidate);
        }
        return granted;
    }
    public void grantRole(String roleName) {
        addRole(roleName);
        setRoleStatus(roleName, "APPROVED");
    }
    public void addRole(String value) {
        String normalized = normalizeRole(value);
        if (normalized == null) return;
        if (roles == null) roles = new ArrayList<>();
        if (!roles.contains(normalized)) roles.add(normalized);
    }
    private static String normalizeRole(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return "VEHICLE_OWNER".equals(normalized) ? "FLEET_OWNER" : normalized;
    }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean value) { available = value; }
    public boolean isFleetOwnerDriverEnabled() { return fleetOwnerDriverEnabled; }
    public void setFleetOwnerDriverEnabled(boolean value) { fleetOwnerDriverEnabled = value; }
    public String getFleetOwnerId() { return fleetOwnerId; }
    public void setFleetOwnerId(String value) { fleetOwnerId = value; }
    public String getPendingFleetOwnerId() { return pendingFleetOwnerId; }
    public void setPendingFleetOwnerId(String value) { pendingFleetOwnerId = value; }
    public String getFleetAssociationStatus() { return fleetAssociationStatus; }
    public void setFleetAssociationStatus(String value) { fleetAssociationStatus = value; }
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
