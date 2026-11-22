package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document("rentalPricingRules")
public class RentalPricingRule {
    @Id private String id;
    private String name;
    private String categoryCode;
    private String vehicleId;
    private String ownerId;
    private String rentalMode;
    private String rentalType;
    private double baseFare;
    private double perHour;
    private double perDay;
    private double includedKm;
    private double perExtraKm;
    private double driverAllowance;
    private double nightAllowance;
    private double securityDeposit;
    private boolean active = true;
    private Instant effectiveFrom;
    private Instant effectiveUntil;
    private Instant updatedAt = Instant.now();

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getCategoryCode(){return categoryCode;} public void setCategoryCode(String v){categoryCode=v;}
    public String getVehicleId(){return vehicleId;} public void setVehicleId(String v){vehicleId=v;}
    public String getOwnerId(){return ownerId;} public void setOwnerId(String v){ownerId=v;}
    public String getRentalMode(){return rentalMode;} public void setRentalMode(String v){rentalMode=v;}
    public String getRentalType(){return rentalType;} public void setRentalType(String v){rentalType=v;}
    public double getBaseFare(){return baseFare;} public void setBaseFare(double v){baseFare=v;}
    public double getPerHour(){return perHour;} public void setPerHour(double v){perHour=v;}
    public double getPerDay(){return perDay;} public void setPerDay(double v){perDay=v;}
    public double getIncludedKm(){return includedKm;} public void setIncludedKm(double v){includedKm=v;}
    public double getPerExtraKm(){return perExtraKm;} public void setPerExtraKm(double v){perExtraKm=v;}
    public double getDriverAllowance(){return driverAllowance;} public void setDriverAllowance(double v){driverAllowance=v;}
    public double getNightAllowance(){return nightAllowance;} public void setNightAllowance(double v){nightAllowance=v;}
    public double getSecurityDeposit(){return securityDeposit;} public void setSecurityDeposit(double v){securityDeposit=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public Instant getEffectiveFrom(){return effectiveFrom;} public void setEffectiveFrom(Instant v){effectiveFrom=v;}
    public Instant getEffectiveUntil(){return effectiveUntil;} public void setEffectiveUntil(Instant v){effectiveUntil=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
