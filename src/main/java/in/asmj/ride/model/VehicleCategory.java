package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Admin-managed vehicle category. Existing CAR/AUTO/BIKE/OTHER values remain valid legacy codes. */
@Document("vehicleCategories")
public class VehicleCategory {
    @Id private String id;
    private String code;
    private String name;
    private String parentCode;
    private String description;
    private boolean active = true;
    private int sortOrder;
    private List<String> supportedRentalModes = new ArrayList<>(List.of("WITH_DRIVER"));
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getParentCode(){return parentCode;} public void setParentCode(String v){parentCode=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public int getSortOrder(){return sortOrder;} public void setSortOrder(int v){sortOrder=v;}
    public List<String> getSupportedRentalModes(){return supportedRentalModes;} public void setSupportedRentalModes(List<String> v){supportedRentalModes=v==null?new ArrayList<>():new ArrayList<>(v);}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
