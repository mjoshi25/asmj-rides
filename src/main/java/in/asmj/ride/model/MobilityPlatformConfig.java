package in.asmj.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Platform-wide defaults for the new on-demand mobility service. */
@Document("mobility_platform_config")
public class MobilityPlatformConfig {
    @Id private String id = "default";
    /** CITY is the selected rollout policy; ServiceCity records determine enabled cities. */
    private String serviceCoverageMode = "CITY";
    /** BOTH enables driver self-acceptance and owner/fleet assignment paths. */
    private String tripAssignmentMode = "BOTH";
    /** CATEGORY means commission is resolved from category-specific rates. */
    private String commissionPolicy = "CATEGORY";
    private double defaultCategoryCommissionPercent = 10.0;
    private Map<String, Double> categoryCommissionPercent = new LinkedHashMap<>();
    private boolean passengerServiceEnabled = true;
    private boolean goodsServiceEnabled = true;
    private Instant updatedAt = Instant.now();

    public MobilityPlatformConfig() {
        categoryCommissionPercent.put("BIKE", 10.0);
        categoryCommissionPercent.put("AUTO", 10.0);
        categoryCommissionPercent.put("CAR", 10.0);
        categoryCommissionPercent.put("SUV", 10.0);
        categoryCommissionPercent.put("VAN", 10.0);
        categoryCommissionPercent.put("BUS", 10.0);
        categoryCommissionPercent.put("PICKUP", 10.0);
        categoryCommissionPercent.put("MINI_TRUCK", 10.0);
        categoryCommissionPercent.put("TRUCK", 10.0);
        categoryCommissionPercent.put("HEAVY_TRUCK", 10.0);
    }

    public String getId(){return id;} public void setId(String v){id=v;}
    public String getServiceCoverageMode(){return serviceCoverageMode;} public void setServiceCoverageMode(String v){serviceCoverageMode=v;touch();}
    public String getTripAssignmentMode(){return tripAssignmentMode;} public void setTripAssignmentMode(String v){tripAssignmentMode=v;touch();}
    public String getCommissionPolicy(){return commissionPolicy;} public void setCommissionPolicy(String v){commissionPolicy=v;touch();}
    public double getDefaultCategoryCommissionPercent(){return defaultCategoryCommissionPercent;} public void setDefaultCategoryCommissionPercent(double v){defaultCategoryCommissionPercent=v;touch();}
    public Map<String,Double> getCategoryCommissionPercent(){return categoryCommissionPercent;} public void setCategoryCommissionPercent(Map<String,Double> v){categoryCommissionPercent=v==null?new LinkedHashMap<>():new LinkedHashMap<>(v);touch();}
    public boolean isPassengerServiceEnabled(){return passengerServiceEnabled;} public void setPassengerServiceEnabled(boolean v){passengerServiceEnabled=v;touch();}
    public boolean isGoodsServiceEnabled(){return goodsServiceEnabled;} public void setGoodsServiceEnabled(boolean v){goodsServiceEnabled=v;touch();}
    public Instant getUpdatedAt(){return updatedAt;} public void touch(){updatedAt=Instant.now();}

    public double commissionFor(String category) {
        if (category == null) return defaultCategoryCommissionPercent;
        return categoryCommissionPercent.getOrDefault(category.trim().toUpperCase(), defaultCategoryCommissionPercent);
    }
}
