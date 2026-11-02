package in.asmj.ride.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
@Document("fare_rules")
public class FareRule {
 @Id private String id="default";
 private double baseFare=40, perKm=12, perMinute=1.5, minimumFare=60, waitingPerMinute=1.5, nightMultiplier=1.25, cancellationFee=30;
 private int nightStartHour=22, nightEndHour=6;
 private double carMultiplier=1.0, autoMultiplier=0.8, bikeMultiplier=0.65, otherMultiplier=1.0;
 private double individualCommissionPercent=15.0, fleetCommissionPercent=10.0;
 private Instant updatedAt=Instant.now();
 public String getId(){return id;} public double getBaseFare(){return baseFare;} public void setBaseFare(double v){baseFare=v;}
 public double getPerKm(){return perKm;} public void setPerKm(double v){perKm=v;} public double getPerMinute(){return perMinute;} public void setPerMinute(double v){perMinute=v;}
 public double getMinimumFare(){return minimumFare;} public void setMinimumFare(double v){minimumFare=v;} public double getWaitingPerMinute(){return waitingPerMinute;} public void setWaitingPerMinute(double v){waitingPerMinute=v;}
 public double getNightMultiplier(){return nightMultiplier;} public void setNightMultiplier(double v){nightMultiplier=v;} public double getCancellationFee(){return cancellationFee;} public void setCancellationFee(double v){cancellationFee=v;}
 public int getNightStartHour(){return nightStartHour;} public void setNightStartHour(int v){nightStartHour=v;} public int getNightEndHour(){return nightEndHour;} public void setNightEndHour(int v){nightEndHour=v;}
 public double getCarMultiplier(){return carMultiplier;} public void setCarMultiplier(double v){carMultiplier=v;} public double getAutoMultiplier(){return autoMultiplier;} public void setAutoMultiplier(double v){autoMultiplier=v;} public double getBikeMultiplier(){return bikeMultiplier;} public void setBikeMultiplier(double v){bikeMultiplier=v;} public double getOtherMultiplier(){return otherMultiplier;} public void setOtherMultiplier(double v){otherMultiplier=v;}
 public double getIndividualCommissionPercent(){return individualCommissionPercent;} public void setIndividualCommissionPercent(double v){individualCommissionPercent=v;} public double getFleetCommissionPercent(){return fleetCommissionPercent;} public void setFleetCommissionPercent(double v){fleetCommissionPercent=v;}
 public Instant getUpdatedAt(){return updatedAt;} public void touch(){updatedAt=Instant.now();}
}
