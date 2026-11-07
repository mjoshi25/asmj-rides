package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

/** Admin-maintained road-distance estimate for a supported city/PIN route. */
@Document("route_distances")
public class RouteDistance {
    @Id private String id;
    private String sourceCityId, sourceCity, sourcePinCode, destinationCityId, destinationCity, destinationPinCode;
    private double distanceKm;
    private String sourceNote;
    private boolean active = true;
    private Instant updatedAt = Instant.now();

    public String getId(){return id;}
    public String getSourceCityId(){return sourceCityId;} public void setSourceCityId(String v){sourceCityId=v;}
    public String getSourceCity(){return sourceCity;} public void setSourceCity(String v){sourceCity=v;}
    public String getSourcePinCode(){return sourcePinCode;} public void setSourcePinCode(String v){sourcePinCode=v;}
    public String getDestinationCityId(){return destinationCityId;} public void setDestinationCityId(String v){destinationCityId=v;}
    public String getDestinationCity(){return destinationCity;} public void setDestinationCity(String v){destinationCity=v;}
    public String getDestinationPinCode(){return destinationPinCode;} public void setDestinationPinCode(String v){destinationPinCode=v;}
    public double getDistanceKm(){return distanceKm;} public void setDistanceKm(double v){distanceKm=v;}
    public String getSourceNote(){return sourceNote;} public void setSourceNote(String v){sourceNote=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void touch(){updatedAt=Instant.now();}
}
