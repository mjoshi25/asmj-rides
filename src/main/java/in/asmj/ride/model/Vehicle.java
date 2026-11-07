package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document("vehicles")
public class Vehicle {
    @Id private String id;
    private String ownerId, category, registrationNumber, model, status="PENDING", assignedDriverId;
    // Legacy operating-city fields retained for older MongoDB documents.
    private String baseCityId, baseCity;
    // Coverage keys use cityId::PIN. Route keys use pickupCityId>dropCityId.
    private List<String> pickupLocationKeys = new ArrayList<>();
    private List<String> dropLocationKeys = new ArrayList<>();
    private List<String> allowedRouteKeys = new ArrayList<>();
    private boolean supportsOneWay = true, supportsRoundTrip = true;
    private String operationalStatus = "ACTIVE"; // ACTIVE, MAINTENANCE, BLOCKED
    private List<UploadedAsset> photos = new ArrayList<>(), documents = new ArrayList<>();

    public String getId(){return id;}
    public String getOwnerId(){return ownerId;} public void setOwnerId(String v){ownerId=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public String getRegistrationNumber(){return registrationNumber;} public void setRegistrationNumber(String v){registrationNumber=v;}
    public String getModel(){return model;} public void setModel(String v){model=v;}
    public String getAssignedDriverId(){return assignedDriverId;} public void setAssignedDriverId(String v){assignedDriverId=v;}
    public String getBaseCityId(){return baseCityId;} public void setBaseCityId(String v){baseCityId=v;}
    public String getBaseCity(){return baseCity;} public void setBaseCity(String v){baseCity=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public List<String> getPickupLocationKeys(){return pickupLocationKeys;} public void setPickupLocationKeys(List<String> v){pickupLocationKeys=v==null?new ArrayList<>():new ArrayList<>(v);}
    public List<String> getDropLocationKeys(){return dropLocationKeys;} public void setDropLocationKeys(List<String> v){dropLocationKeys=v==null?new ArrayList<>():new ArrayList<>(v);}
    public List<String> getAllowedRouteKeys(){return allowedRouteKeys;} public void setAllowedRouteKeys(List<String> v){allowedRouteKeys=v==null?new ArrayList<>():new ArrayList<>(v);}
    public boolean isSupportsOneWay(){return supportsOneWay;} public void setSupportsOneWay(boolean v){supportsOneWay=v;}
    public boolean isSupportsRoundTrip(){return supportsRoundTrip;} public void setSupportsRoundTrip(boolean v){supportsRoundTrip=v;}
    public String getOperationalStatus(){return operationalStatus;} public void setOperationalStatus(String v){operationalStatus=v;}
    public List<UploadedAsset> getPhotos(){return photos;} public void setPhotos(List<UploadedAsset> v){photos=v==null?new ArrayList<>():v;}
    public List<UploadedAsset> getDocuments(){return documents;} public void setDocuments(List<UploadedAsset> v){documents=v==null?new ArrayList<>():v;}
}
