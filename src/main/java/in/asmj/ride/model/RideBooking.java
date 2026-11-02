package in.asmj.ride.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.time.LocalDateTime;
@Document("rides")
public class RideBooking {
 @Id private String id;
 private String customerId, customerName, customerMobile, pickup, destination, category, tripType, status="REQUESTED", paymentMethod="CASH", assignedDriverId, assignedDriverName, assignedVehicleId, assignedVehicleNumber, dispatchNote;
 private double estimatedDistanceKm, estimatedFare, actualDistanceKm, finalFare, currentDriverLatitude, currentDriverLongitude;
 private int actualDurationMinutes, waitingMinutes; private String pickupOtp; private Instant driverLocationUpdatedAt, tripStartedAt, tripCompletedAt; private double cancellationCharge;
 private LocalDateTime scheduledAt;
 private Instant createdAt=Instant.now(), updatedAt=Instant.now();
 public String getId(){return id;} public String getCustomerId(){return customerId;} public void setCustomerId(String v){customerId=v;}
 public String getCustomerName(){return customerName;} public void setCustomerName(String v){customerName=v;}
 public String getCustomerMobile(){return customerMobile;} public void setCustomerMobile(String v){customerMobile=v;}
 public String getPickup(){return pickup;} public void setPickup(String v){pickup=v;}
 public String getDestination(){return destination;} public void setDestination(String v){destination=v;}
 public String getCategory(){return category;} public void setCategory(String v){category=v;}
 public String getTripType(){return tripType;} public void setTripType(String v){tripType=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;updatedAt=Instant.now();}
 public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;}
 public String getAssignedDriverId(){return assignedDriverId;} public void setAssignedDriverId(String v){assignedDriverId=v;}
 public String getAssignedDriverName(){return assignedDriverName;} public void setAssignedDriverName(String v){assignedDriverName=v;}
 public String getAssignedVehicleId(){return assignedVehicleId;} public void setAssignedVehicleId(String v){assignedVehicleId=v;}
 public String getAssignedVehicleNumber(){return assignedVehicleNumber;} public void setAssignedVehicleNumber(String v){assignedVehicleNumber=v;}
 public String getDispatchNote(){return dispatchNote;} public void setDispatchNote(String v){dispatchNote=v;}
 public double getEstimatedDistanceKm(){return estimatedDistanceKm;} public void setEstimatedDistanceKm(double v){estimatedDistanceKm=v;}
 public double getEstimatedFare(){return estimatedFare;} public void setEstimatedFare(double v){estimatedFare=v;}
 public LocalDateTime getScheduledAt(){return scheduledAt;} public void setScheduledAt(LocalDateTime v){scheduledAt=v;}
 public double getActualDistanceKm(){return actualDistanceKm;} public void setActualDistanceKm(double v){actualDistanceKm=v;}
 public double getFinalFare(){return finalFare;} public void setFinalFare(double v){finalFare=v;}
 public int getActualDurationMinutes(){return actualDurationMinutes;} public void setActualDurationMinutes(int v){actualDurationMinutes=v;}
 public int getWaitingMinutes(){return waitingMinutes;} public void setWaitingMinutes(int v){waitingMinutes=v;}
 public String getPickupOtp(){return pickupOtp;} public void setPickupOtp(String v){pickupOtp=v;}
 public double getCurrentDriverLatitude(){return currentDriverLatitude;} public void setCurrentDriverLatitude(double v){currentDriverLatitude=v;}
 public double getCurrentDriverLongitude(){return currentDriverLongitude;} public void setCurrentDriverLongitude(double v){currentDriverLongitude=v;}
 public Instant getDriverLocationUpdatedAt(){return driverLocationUpdatedAt;} public void setDriverLocationUpdatedAt(Instant v){driverLocationUpdatedAt=v;}
 public Instant getTripStartedAt(){return tripStartedAt;} public void setTripStartedAt(Instant v){tripStartedAt=v;}
 public Instant getTripCompletedAt(){return tripCompletedAt;} public void setTripCompletedAt(Instant v){tripCompletedAt=v;}
 public double getCancellationCharge(){return cancellationCharge;} public void setCancellationCharge(double v){cancellationCharge=v;}
 public Instant getUpdatedAt(){return updatedAt;} public Instant getCreatedAt(){return createdAt;}
}
