package in.asmj.ride.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Document("service_cities")
public class ServiceCity {
 @Id private String id; private String city,district,state; private boolean active=true; private List<String> pinCodes=new ArrayList<>(); private List<String> enabledCategoryCodes=new ArrayList<>(); private boolean categorySelectionConfigured=false; private Instant createdAt=Instant.now(),updatedAt=Instant.now();
 public String getId(){return id;} public String getCity(){return city;} public void setCity(String v){city=v;} public String getDistrict(){return district;} public void setDistrict(String v){district=v;} public String getState(){return state;} public void setState(String v){state=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;} public List<String> getPinCodes(){return pinCodes;} public void setPinCodes(List<String> v){pinCodes=v==null?new ArrayList<>():v;} public List<String> getEnabledCategoryCodes(){return enabledCategoryCodes;} public void setEnabledCategoryCodes(List<String> v){enabledCategoryCodes=v==null?new ArrayList<>():new ArrayList<>(v);} public boolean isCategorySelectionConfigured(){return categorySelectionConfigured;} public void setCategorySelectionConfigured(boolean v){categorySelectionConfigured=v;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public void touch(){updatedAt=Instant.now();}
}
