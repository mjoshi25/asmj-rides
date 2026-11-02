package in.asmj.ride.model;
import org.springframework.data.annotation.Id;import org.springframework.data.mongodb.core.mapping.Document;
@Document("vehicles") public class Vehicle {
 @Id private String id;private String ownerId,category,registrationNumber,model,status="PENDING",assignedDriverId;
 public String getId(){return id;}public String getOwnerId(){return ownerId;}public void setOwnerId(String x){ownerId=x;}public String getCategory(){return category;}public void setCategory(String x){category=x;}public String getRegistrationNumber(){return registrationNumber;}public void setRegistrationNumber(String x){registrationNumber=x;}public String getModel(){return model;}public void setModel(String x){model=x;}public String getAssignedDriverId(){return assignedDriverId;}public void setAssignedDriverId(String x){assignedDriverId=x;}public String getStatus(){return status;}public void setStatus(String x){status=x;}
}