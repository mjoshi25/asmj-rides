package in.asmj.ride.service;
import in.asmj.ride.model.FareRule;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
@Component
public class FareCalculator {
 public double categoryMultiplier(FareRule f,String category){return switch(category){case "AUTO"->f.getAutoMultiplier();case "BIKE"->f.getBikeMultiplier();case "OTHER"->f.getOtherMultiplier();default->f.getCarMultiplier();};}
 public double estimate(FareRule f,String category,double km){double raw=(f.getBaseFare()+Math.max(0,km)*f.getPerKm())*categoryMultiplier(f,category);return round(Math.max(f.getMinimumFare(),raw));}
 public double finalFare(FareRule f,String category,double km,int minutes,int waiting,LocalDateTime startedAt){double multiplier=categoryMultiplier(f,category);double raw=(f.getBaseFare()+Math.max(0,km)*f.getPerKm()+Math.max(0,minutes)*f.getPerMinute()+Math.max(0,waiting)*f.getWaitingPerMinute())*multiplier;if(startedAt!=null){int h=startedAt.getHour();boolean night=f.getNightStartHour()>f.getNightEndHour()?(h>=f.getNightStartHour()||h<f.getNightEndHour()):(h>=f.getNightStartHour()&&h<f.getNightEndHour());if(night)raw*=f.getNightMultiplier();}return round(Math.max(f.getMinimumFare(),raw));}
 private double round(double v){return Math.round(v*100.0)/100.0;}
}
