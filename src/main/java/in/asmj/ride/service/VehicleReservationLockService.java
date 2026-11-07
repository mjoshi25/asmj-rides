package in.asmj.ride.service;

import in.asmj.ride.model.VehicleBookingLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

/** Short MongoDB-backed distributed mutex for vehicles, drivers and ride requests. Availability and state are rechecked while locks are held. */
@Service
public class VehicleReservationLockService {
    private final MongoTemplate mongo;
    public VehicleReservationLockService(MongoTemplate mongo){this.mongo=mongo;}

    public String tryAcquire(String resourceId){
        if(resourceId==null||resourceId.isBlank())return null;
        Query byId=new Query(Criteria.where("_id").is(resourceId));
        if(!mongo.exists(byId,VehicleBookingLock.class)){
            VehicleBookingLock initial=new VehicleBookingLock();initial.setId(resourceId);initial.setLockUntil(Instant.EPOCH);initial.setExpiresAt(Instant.now().plusSeconds(180));
            try{mongo.insert(initial);}catch(DuplicateKeyException racedCreation){/* another request created the lock row */}
        }
        Instant now=Instant.now();String token=UUID.randomUUID().toString();
        Query available=new Query(new Criteria().andOperator(Criteria.where("_id").is(resourceId),Criteria.where("lockUntil").lt(now)));
        Update update=new Update().set("token",token).set("lockUntil",now.plusSeconds(120)).set("expiresAt",now.plusSeconds(180));
        VehicleBookingLock result=mongo.findAndModify(available,update,FindAndModifyOptions.options().returnNew(true),VehicleBookingLock.class);
        return result!=null&&token.equals(result.getToken())?token:null;
    }
    public String tryAcquireRide(String rideId){return tryAcquire("ride:"+rideId);}
    public String tryAcquireDriver(String driverId){return tryAcquire("driver:"+driverId);}
    public void releaseRide(String rideId,String token){release("ride:"+rideId,token);}
    public void releaseDriver(String driverId,String token){release("driver:"+driverId,token);}

    public void release(String resourceId,String token){
        if(resourceId==null||token==null)return;
        Query query=new Query(Criteria.where("_id").is(resourceId).and("token").is(token));
        mongo.updateFirst(query,new Update().set("lockUntil",Instant.EPOCH).set("expiresAt",Instant.now().plusSeconds(60)),VehicleBookingLock.class);
    }
}
