package in.asmj.ride.model;
import java.time.Instant;
public class TripStatusEvent {
 private String fromStatus,toStatus,actor,note; private Instant changedAt=Instant.now();
 public TripStatusEvent(){} public TripStatusEvent(String from,String to,String actor,String note,Instant at){this.fromStatus=from;this.toStatus=to;this.actor=actor;this.note=note;this.changedAt=at;}
 public String getFromStatus(){return fromStatus;} public String getToStatus(){return toStatus;} public String getActor(){return actor;} public String getNote(){return note;} public Instant getChangedAt(){return changedAt;}
}
