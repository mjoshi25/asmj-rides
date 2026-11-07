package in.asmj.ride.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("vehicle_booking_locks")
public class VehicleBookingLock {
 @Id private String id; private String token; private Instant lockUntil;
 public String getId(){return id;} public void setId(String v){id=v;}
 public String getToken(){return token;} public void setToken(String v){token=v;}
 public Instant getLockUntil(){return lockUntil;} public void setLockUntil(Instant v){lockUntil=v;}
}
