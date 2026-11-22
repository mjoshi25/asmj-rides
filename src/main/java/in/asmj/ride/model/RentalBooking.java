package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.time.LocalDateTime;

/** Separate rental lifecycle; legacy RideBooking records are not migrated or overwritten. */
@Document("rentalBookings")
public class RentalBooking {
    @Id private String id;
    private String customerId;
    private String vehicleId;
    private String vehicleOwnerId;
    private String managingAgentId;
    private String bookingAcceptedById;
    private String bookingAcceptedByRole; // VEHICLE_OWNER or TRAVEL_AGENT
    private String rentalMode = "WITH_DRIVER"; // WITH_DRIVER or SELF_DRIVE
    private String rentalType; // HOURLY, DAILY, LOCAL, OUTSTATION, ONE_WAY, ROUND_TRIP, LONG_TERM, COMMERCIAL
    private LocalDateTime pickupAt;
    private LocalDateTime returnAt;
    private String pickupAddress;
    private String dropAddress;
    private String status = "REQUESTED";
    private String paymentStatus = "PENDING";
    private double estimatedAmount;
    private double quotedAmount;
    private double finalAmount;
    private double securityDeposit;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getCustomerId(){return customerId;} public void setCustomerId(String v){customerId=v;}
    public String getVehicleId(){return vehicleId;} public void setVehicleId(String v){vehicleId=v;}
    public String getVehicleOwnerId(){return vehicleOwnerId;} public void setVehicleOwnerId(String v){vehicleOwnerId=v;}
    public String getManagingAgentId(){return managingAgentId;} public void setManagingAgentId(String v){managingAgentId=v;}
    public String getBookingAcceptedById(){return bookingAcceptedById;} public void setBookingAcceptedById(String v){bookingAcceptedById=v;}
    public String getBookingAcceptedByRole(){return bookingAcceptedByRole;} public void setBookingAcceptedByRole(String v){bookingAcceptedByRole=v;}
    public String getRentalMode(){return rentalMode;} public void setRentalMode(String v){rentalMode=v;}
    public String getRentalType(){return rentalType;} public void setRentalType(String v){rentalType=v;}
    public LocalDateTime getPickupAt(){return pickupAt;} public void setPickupAt(LocalDateTime v){pickupAt=v;}
    public LocalDateTime getReturnAt(){return returnAt;} public void setReturnAt(LocalDateTime v){returnAt=v;}
    public String getPickupAddress(){return pickupAddress;} public void setPickupAddress(String v){pickupAddress=v;}
    public String getDropAddress(){return dropAddress;} public void setDropAddress(String v){dropAddress=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;updatedAt=Instant.now();}
    public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String v){paymentStatus=v;}
    public double getEstimatedAmount(){return estimatedAmount;} public void setEstimatedAmount(double v){estimatedAmount=v;}
    public double getQuotedAmount(){return quotedAmount;} public void setQuotedAmount(double v){quotedAmount=v;}
    public double getFinalAmount(){return finalAmount;} public void setFinalAmount(double v){finalAmount=v;}
    public double getSecurityDeposit(){return securityDeposit;} public void setSecurityDeposit(double v){securityDeposit=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
