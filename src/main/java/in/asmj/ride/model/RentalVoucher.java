package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("rentalVouchers")
public class RentalVoucher {
 @Id private String id;
 private String code, providerId, providerRole, discountType, status="ACTIVE", title, description;
 private List<String> vehicleIds=new ArrayList<>();
 private double discountValue, minimumBookingAmount, maximumDiscountAmount;
 private int maximumUses, maximumUsesPerCustomer=1, redeemedCount;
 private Instant startsAt, expiresAt, createdAt=Instant.now();
 public String getId(){return id;} public void setId(String v){id=v;}
 public String getCode(){return code;} public void setCode(String v){code=v;}
 public String getProviderId(){return providerId;} public void setProviderId(String v){providerId=v;}
 public String getProviderRole(){return providerRole;} public void setProviderRole(String v){providerRole=v;}
 public String getDiscountType(){return discountType;} public void setDiscountType(String v){discountType=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public String getDescription(){return description;} public void setDescription(String v){description=v;}
 public List<String> getVehicleIds(){return vehicleIds;} public void setVehicleIds(List<String> v){vehicleIds=v==null?new ArrayList<>():new ArrayList<>(v);}
 public double getDiscountValue(){return discountValue;} public void setDiscountValue(double v){discountValue=v;}
 public double getMinimumBookingAmount(){return minimumBookingAmount;} public void setMinimumBookingAmount(double v){minimumBookingAmount=v;}
 public double getMaximumDiscountAmount(){return maximumDiscountAmount;} public void setMaximumDiscountAmount(double v){maximumDiscountAmount=v;}
 public int getMaximumUses(){return maximumUses;} public void setMaximumUses(int v){maximumUses=v;}
 public int getMaximumUsesPerCustomer(){return maximumUsesPerCustomer;} public void setMaximumUsesPerCustomer(int v){maximumUsesPerCustomer=v;}
 public int getRedeemedCount(){return redeemedCount;} public void setRedeemedCount(int v){redeemedCount=v;}
 public Instant getStartsAt(){return startsAt;} public void setStartsAt(Instant v){startsAt=v;}
 public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant v){expiresAt=v;}
 public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
