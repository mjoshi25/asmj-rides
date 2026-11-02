package in.asmj.ride.model;
import org.springframework.data.annotation.Id;import org.springframework.data.mongodb.core.mapping.Document;import java.time.Instant;
@Document("ride_reviews") public class RideReview {
 @Id private String id; private String rideId,customerId,driverId,comment; private int customerRating,driverRating; private Instant createdAt=Instant.now();
 public String getId(){return id;} public String getRideId(){return rideId;} public void setRideId(String v){rideId=v;} public String getCustomerId(){return customerId;} public void setCustomerId(String v){customerId=v;} public String getDriverId(){return driverId;} public void setDriverId(String v){driverId=v;} public String getComment(){return comment;} public void setComment(String v){comment=v;} public int getCustomerRating(){return customerRating;} public void setCustomerRating(int v){customerRating=v;} public int getDriverRating(){return driverRating;} public void setDriverRating(int v){driverRating=v;} public Instant getCreatedAt(){return createdAt;}
}
