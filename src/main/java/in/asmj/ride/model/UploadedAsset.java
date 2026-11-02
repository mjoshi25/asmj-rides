package in.asmj.ride.model;

import java.time.Instant;
import java.util.UUID;

/** Embedded Cloudinary asset metadata. Private assets use authenticated delivery. */
public class UploadedAsset {
    private String id = UUID.randomUUID().toString();
    private String category;
    private String originalFilename;
    private String publicId;
    private String secureUrl;
    private String resourceType;
    private String deliveryType;
    private String contentType;
    private long bytes;
    private String verificationStatus = "PENDING";
    private String adminNote;
    private Instant uploadedAt = Instant.now();

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public String getOriginalFilename(){return originalFilename;} public void setOriginalFilename(String v){originalFilename=v;}
    public String getPublicId(){return publicId;} public void setPublicId(String v){publicId=v;}
    public String getSecureUrl(){return secureUrl;} public void setSecureUrl(String v){secureUrl=v;}
    public String getResourceType(){return resourceType;} public void setResourceType(String v){resourceType=v;}
    public String getDeliveryType(){return deliveryType;} public void setDeliveryType(String v){deliveryType=v;}
    public String getContentType(){return contentType;} public void setContentType(String v){contentType=v;}
    public long getBytes(){return bytes;} public void setBytes(long v){bytes=v;}
    public String getVerificationStatus(){return verificationStatus;} public void setVerificationStatus(String v){verificationStatus=v;}
    public String getAdminNote(){return adminNote;} public void setAdminNote(String v){adminNote=v;}
    public Instant getUploadedAt(){return uploadedAt;} public void setUploadedAt(Instant v){uploadedAt=v;}
}
