package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document("ride_notifications")
public class RideNotification {
    @Id private String id;
    private String recipientId, title, message, link, rideId, type;
    private boolean read;
    private Instant createdAt = Instant.now();

    public String getId(){return id;}
    public String getRecipientId(){return recipientId;} public void setRecipientId(String v){recipientId=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getMessage(){return message;} public void setMessage(String v){message=v;}
    public String getLink(){return link;} public void setLink(String v){link=v;}
    public String getRideId(){return rideId;} public void setRideId(String v){rideId=v;}
    public String getType(){return type;} public void setType(String v){type=v;}
    public boolean isRead(){return read;} public void setRead(boolean v){read=v;}
    public Instant getCreatedAt(){return createdAt;}
}
