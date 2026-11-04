package in.asmj.ride.model;
import java.time.Instant;
public class PaymentStatusEvent {
 private String fromStatus,toStatus,actor,note; private Instant changedAt=Instant.now();
 public PaymentStatusEvent(){} public PaymentStatusEvent(String from,String to,String actor,String note){this.fromStatus=from;this.toStatus=to;this.actor=actor;this.note=note;this.changedAt=Instant.now();}
 public String getFromStatus(){return fromStatus;} public String getToStatus(){return toStatus;} public String getActor(){return actor;} public String getNote(){return note;} public Instant getChangedAt(){return changedAt;}
}
