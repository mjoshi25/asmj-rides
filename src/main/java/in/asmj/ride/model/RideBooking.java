package in.asmj.ride.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Document("rides")
public class RideBooking {
 @Id private String id;
 private String customerId, customerName, customerMobile, pickup, destination, category, tripType, status="REQUESTED", paymentMethod="CASH", assignedDriverId, assignedDriverName, assignedVehicleId, assignedVehicleNumber, dispatchNote;
 private String requestedVehicleId, requestedVehicleNumber; private boolean fareEstimateAccepted; private Instant fareEstimateAcceptedAt;
 private String journeyType="ONE_WAY", pickupCityId, pickupCity, pickupPinCode, destinationCityId, destinationCity, destinationPinCode, returnLegStatus="NOT_APPLICABLE", returnAssignedDriverId, returnAssignedDriverName, returnAssignedVehicleId, returnAssignedVehicleNumber, returnPickupOtp;
 private LocalDateTime returnAt; private Instant returnTripStartedAt, returnTripCompletedAt; private double returnActualDistanceKm, returnFinalFare;
 private double estimatedDistanceKm, estimatedFare, actualDistanceKm, finalFare, currentDriverLatitude, currentDriverLongitude;
 private int actualDurationMinutes, waitingMinutes; private String pickupOtp; private Instant driverLocationUpdatedAt, tripStartedAt, tripCompletedAt; private double cancellationCharge;
 private LocalDateTime scheduledAt;
 private Instant createdAt=Instant.now(), updatedAt=Instant.now();
 private List<TripStatusEvent> statusHistory=new ArrayList<>();
 public List<TripStatusEvent> getStatusHistory(){return statusHistory;} public void setStatusHistory(List<TripStatusEvent> v){statusHistory=v==null?new ArrayList<>():v;}
 public void addStatusEvent(String from,String to,String actor,String note){if(statusHistory==null)statusHistory=new ArrayList<>();statusHistory.add(new TripStatusEvent(from,to,actor,note,Instant.now()));}

 public String getRequestedVehicleId(){return requestedVehicleId;} public void setRequestedVehicleId(String v){requestedVehicleId=v;} public String getRequestedVehicleNumber(){return requestedVehicleNumber;} public void setRequestedVehicleNumber(String v){requestedVehicleNumber=v;} public boolean isFareEstimateAccepted(){return fareEstimateAccepted;} public void setFareEstimateAccepted(boolean v){fareEstimateAccepted=v;} public Instant getFareEstimateAcceptedAt(){return fareEstimateAcceptedAt;} public void setFareEstimateAcceptedAt(Instant v){fareEstimateAcceptedAt=v;}
 public String getJourneyType(){return journeyType;} public void setJourneyType(String v){journeyType=v;}
 public String getPickupCityId(){return pickupCityId;} public void setPickupCityId(String v){pickupCityId=v;} public String getPickupCity(){return pickupCity;} public void setPickupCity(String v){pickupCity=v;}
 public String getPickupPinCode(){return pickupPinCode;} public void setPickupPinCode(String v){pickupPinCode=v;} public String getDestinationCityId(){return destinationCityId;} public void setDestinationCityId(String v){destinationCityId=v;} public String getDestinationCity(){return destinationCity;} public void setDestinationCity(String v){destinationCity=v;} public String getDestinationPinCode(){return destinationPinCode;} public void setDestinationPinCode(String v){destinationPinCode=v;}
 public String getReturnLegStatus(){return returnLegStatus;} public void setReturnLegStatus(String v){returnLegStatus=v;} public String getReturnAssignedDriverId(){return returnAssignedDriverId;} public void setReturnAssignedDriverId(String v){returnAssignedDriverId=v;} public String getReturnAssignedDriverName(){return returnAssignedDriverName;} public void setReturnAssignedDriverName(String v){returnAssignedDriverName=v;} public String getReturnAssignedVehicleId(){return returnAssignedVehicleId;} public void setReturnAssignedVehicleId(String v){returnAssignedVehicleId=v;} public String getReturnAssignedVehicleNumber(){return returnAssignedVehicleNumber;} public void setReturnAssignedVehicleNumber(String v){returnAssignedVehicleNumber=v;} public String getReturnPickupOtp(){return returnPickupOtp;} public void setReturnPickupOtp(String v){returnPickupOtp=v;} public LocalDateTime getReturnAt(){return returnAt;} public void setReturnAt(LocalDateTime v){returnAt=v;} public Instant getReturnTripStartedAt(){return returnTripStartedAt;} public void setReturnTripStartedAt(Instant v){returnTripStartedAt=v;} public Instant getReturnTripCompletedAt(){return returnTripCompletedAt;} public void setReturnTripCompletedAt(Instant v){returnTripCompletedAt=v;} public double getReturnActualDistanceKm(){return returnActualDistanceKm;} public void setReturnActualDistanceKm(double v){returnActualDistanceKm=v;} public double getReturnFinalFare(){return returnFinalFare;} public void setReturnFinalFare(double v){returnFinalFare=v;}
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
