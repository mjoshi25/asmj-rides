package in.asmj.ride.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A single user/admin message embedded in a support conversation. */
public class SupportMessage {
    private String senderRole;
    private String senderName;
    private String text;
    private List<UploadedAsset> attachments = new ArrayList<>();
    private Instant createdAt = Instant.now();

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String value) { senderRole = value; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String value) { senderName = value; }
    public String getText() { return text; }
    public void setText(String value) { text = value; }
    public List<UploadedAsset> getAttachments() { return attachments; }
    public void setAttachments(List<UploadedAsset> value) { attachments = value == null ? new ArrayList<>() : value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
}
