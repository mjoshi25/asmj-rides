package in.asmj.ride.service;

import in.asmj.ride.model.*;
import in.asmj.ride.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import java.util.*;

/** Creates fictional, clearly watermarked demo accounts and assets only when explicitly enabled. */
@Component
@Profile("!prod & !production")
@ConditionalOnProperty(name="demo.seed.enabled",havingValue="true")
public class DemoDataSeeder implements CommandLineRunner {
    private final UserAccountRepository users; private final VehicleRepository vehicles;
    private final ServiceCityRepository cities; private final RouteDistanceRepository routes;
    private final PasswordEncoder encoder; private final String password;

    public DemoDataSeeder(UserAccountRepository users,VehicleRepository vehicles,ServiceCityRepository cities,
                          RouteDistanceRepository routes,PasswordEncoder encoder,
                          @Value("${demo.seed.password:}") String password){
        this.users=users;this.vehicles=vehicles;this.cities=cities;this.routes=routes;this.encoder=encoder;this.password=password;
    }

    @Override public void run(String... args){
        if(password==null||password.length()<12){System.err.println("ASMJ Ride demo seed skipped: DEMO_SEED_PASSWORD must be at least 12 characters.");return;}
        ServiceCity bhawanipatna=city("Bhawanipatna","Kalahandi","Odisha",List.of("766001"));
        ServiceCity kesinga=city("Kesinga","Kalahandi","Odisha",List.of("766012"));
        ServiceCity junagarh=city("Junagarh","Kalahandi","Odisha",List.of("766014"));
        ServiceCity bhubaneswar=city("Bhubaneswar","Khordha","Odisha",List.of("751001"));
        ServiceCity brahmapur=city("Brahmapur","Ganjam","Odisha",List.of("760004"));
        ServiceCity sambalpur=city("Sambalpur","Sambalpur","Odisha",List.of("768001"));
        seedRoute(bhawanipatna,"766001",brahmapur,"760004",270);
        seedRoute(bhawanipatna,"766001",bhubaneswar,"751001",360);
        seedRoute(bhawanipatna,"766001",kesinga,"766012",35);
        seedRoute(kesinga,"766012",sambalpur,"768001",155);
        seedRoute(junagarh,"766014",bhubaneswar,"751001",380);
        seedRoute(bhawanipatna,"766001",sambalpur,"768001",180);

        UserAccount fleet1=user("Demo Fleet Owner 1","demo.fleetowner1@example.invalid","0000000001","FLEET_OWNER",null,List.of("IDENTITY","BUSINESS_REGISTRATION"));
        UserAccount fleet2=user("Demo Fleet Owner 2","demo.fleetowner2@example.invalid","0000000002","FLEET_OWNER",null,List.of("IDENTITY","BUSINESS_REGISTRATION"));
        UserAccount driver1=user("Demo Driver 1","demo.driver1@example.invalid","0000000003","DRIVER",fleet1.getId(),List.of("IDENTITY","DRIVING_LICENCE"));
        UserAccount driver2=user("Demo Driver 2","demo.driver2@example.invalid","0000000004","DRIVER",fleet1.getId(),List.of("IDENTITY","DRIVING_LICENCE"));
        UserAccount driver3=user("Demo Driver 3","demo.driver3@example.invalid","0000000005","DRIVER",fleet1.getId(),List.of("IDENTITY","DRIVING_LICENCE"));
        UserAccount driver4=user("Demo Driver 4","demo.driver4@example.invalid","0000000006","DRIVER",fleet2.getId(),List.of("IDENTITY","DRIVING_LICENCE"));
        user("Demo Customer 1","demo.customer1@example.invalid","0000000007","CUSTOMER",null,List.of());
        user("Demo Customer 2","demo.customer2@example.invalid","0000000008","CUSTOMER",null,List.of());
        user("Demo Customer 3","demo.customer3@example.invalid","0000000009","CUSTOMER",null,List.of());

        List<String> car1Areas=List.of(key(bhawanipatna,"766001"),key(bhubaneswar,"751001"),key(brahmapur,"760004"));
        List<String> car2Areas=List.of(key(bhawanipatna,"766001"),key(kesinga,"766012"),key(sambalpur,"768001"));
        List<String> car3Areas=List.of(key(junagarh,"766014"),key(bhubaneswar,"751001"),key(bhawanipatna,"766001"),key(sambalpur,"768001"));
        List<String> localAreas=List.of(key(bhawanipatna,"766001"),key(kesinga,"766012"));
        List<String> bikeAreas=List.of(key(junagarh,"766014"),key(kesinga,"766012"),key(bhawanipatna,"766001"));
        vehicle(fleet1,driver1,"CAR","OD08DEMO001","Demo Compact Sedan",car1Areas,car1Areas,List.of(route(bhawanipatna,brahmapur),route(brahmapur,bhawanipatna),route(bhawanipatna,bhubaneswar),route(bhubaneswar,bhawanipatna)),"demo-car-01");
        vehicle(fleet1,driver2,"CAR","OD08DEMO002","Demo Family Sedan",car2Areas,car2Areas,List.of(route(bhawanipatna,kesinga),route(kesinga,bhawanipatna),route(kesinga,sambalpur),route(sambalpur,kesinga)),"demo-car-02");
        vehicle(fleet1,driver3,"CAR","OD08DEMO003","Demo SUV",car3Areas,car3Areas,List.of(route(junagarh,bhubaneswar),route(bhubaneswar,junagarh),route(bhawanipatna,sambalpur),route(sambalpur,bhawanipatna)),"demo-car-03");
        vehicle(fleet2,driver4,"AUTO","OD08DEMO004","Demo Passenger Auto",localAreas,localAreas,List.of(route(bhawanipatna,kesinga),route(kesinga,bhawanipatna)),"demo-auto-04");
        vehicle(fleet2,null,"BIKE","OD08DEMO005","Demo Commuter Bike",bikeAreas,bikeAreas,List.of(route(junagarh,kesinga),route(kesinga,junagarh),route(bhawanipatna,kesinga),route(kesinga,bhawanipatna)),"demo-bike-05");
        System.out.println("ASMJ Ride demo seed completed. Demo accounts: 2 fleet owners, 4 drivers, 3 customers; 5 sample vehicles.");
    }

    private ServiceCity city(String name,String district,String state,List<String> pins){
        ServiceCity found=cities.findAllByOrderByCityAsc().stream().filter(c->name.equalsIgnoreCase(c.getCity())).findFirst().orElse(null);
        if(found!=null){if(found.getPinCodes()==null)found.setPinCodes(new ArrayList<>());boolean changed=false;if(!found.isActive()){found.setActive(true);changed=true;}for(String pin:pins)if(!found.getPinCodes().contains(pin)){found.getPinCodes().add(pin);changed=true;}if(changed)cities.save(found);return found;}
        ServiceCity c=new ServiceCity();c.setCity(name);c.setDistrict(district);c.setState(state);c.setPinCodes(new ArrayList<>(pins));c.setActive(true);return cities.save(c);
    }
    private void seedRoute(ServiceCity from,String fromPin,ServiceCity to,String toPin,double km){
        boolean exists=routes.findAll().stream().anyMatch(r->from.getId().equals(r.getSourceCityId())&&to.getId().equals(r.getDestinationCityId())&&fromPin.equals(r.getSourcePinCode())&&toPin.equals(r.getDestinationPinCode()));
        if(exists)return;RouteDistance r=new RouteDistance();r.setSourceCityId(from.getId());r.setSourceCity(from.getCity());r.setSourcePinCode(fromPin);r.setDestinationCityId(to.getId());r.setDestinationCity(to.getCity());r.setDestinationPinCode(toPin);r.setDistanceKm(km);r.setSourceNote("DEMO SAMPLE DISTANCE — verify with a current road-distance source before production use.");r.setActive(true);routes.save(r);
    }
    private UserAccount user(String name,String email,String mobile,String role,String fleetOwnerId,List<String> documentTypes){
        UserAccount u=users.findByEmailIgnoreCase(email).orElse(null);
        if(u!=null&&!role.equals(u.getRole()))throw new IllegalStateException("Demo email already belongs to a different role: "+email);
        if(u==null){u=new UserAccount();u.setName(name);u.setEmail(email);u.setMobile(mobile);u.setPasswordHash(encoder.encode(password));u.setRole(role);}
        // Repair reserved demo accounts on repeat runs; do not reset existing demo passwords.
        if(u.getDocuments()==null)u.setDocuments(new ArrayList<>());u.setStatus("APPROVED");u.setMobileVerified(true);if(u.getMobileVerifiedAt()==null)u.setMobileVerifiedAt(java.time.Instant.now());u.setMobileVerificationMethod("DEMO_SEED");u.setAvailable("DRIVER".equals(role));if(fleetOwnerId!=null)u.setFleetOwnerId(fleetOwnerId);
        for(String type:documentTypes){
            UploadedAsset existing=u.getDocuments().stream().filter(d->type.equals(d.getCategory())).findFirst().orElse(null);
            if(existing==null||existing.getPublicId()==null||!existing.getPublicId().startsWith("demo:")){
                u.getDocuments().removeIf(d->type.equals(d.getCategory()));String file=email.substring(0,email.indexOf('@')).replaceFirst("^demo\\.","")+"-"+type.toLowerCase(Locale.ROOT).replace('_','-')+"-sample.pdf";u.getDocuments().add(asset(type,file,"application/pdf"));
            }else{existing.setVerificationStatus("VERIFIED");existing.setAdminNote("Fictional demo sample — not a real identity, registration, licence or insurance document.");}
        }
        return users.save(u);
    }
    private void vehicle(UserAccount owner,UserAccount driver,String category,String reg,String model,List<String> pickups,List<String> drops,List<String> routeKeys,String imageStem){
        Vehicle v=vehicles.findAll().stream().filter(x->reg.equalsIgnoreCase(x.getRegistrationNumber())).findFirst().orElse(null);if(v==null)v=new Vehicle();else if(!owner.getId().equals(v.getOwnerId()))throw new IllegalStateException("Reserved demo registration already belongs to another owner: "+reg);
        if(v.getPhotos()==null)v.setPhotos(new ArrayList<>());if(v.getDocuments()==null)v.setDocuments(new ArrayList<>());v.setOwnerId(owner.getId());v.setCategory(category);v.setRegistrationNumber(reg);v.setModel(model);v.setStatus("APPROVED");v.setOperationalStatus("ACTIVE");v.setAssignedDriverId(driver==null?null:driver.getId());v.setPickupLocationKeys(pickups);v.setDropLocationKeys(drops);v.setAllowedRouteKeys(routeKeys);v.setSupportsOneWay(true);v.setSupportsRoundTrip(true);
        ServiceCity base=cities.findAllByOrderByCityAsc().stream().filter(c->"Bhawanipatna".equals(c.getCity())).findFirst().orElse(null);if(base!=null){v.setBaseCityId(base.getId());v.setBaseCity(base.getCity());}
        UploadedAsset photo=v.getPhotos().stream().filter(p->"VEHICLE_PHOTO".equals(p.getCategory())).findFirst().orElse(null);
        if(photo==null||photo.getPublicId()==null||!photo.getPublicId().startsWith("demo:")){v.getPhotos().removeIf(p->"VEHICLE_PHOTO".equals(p.getCategory()));v.getPhotos().add(asset("VEHICLE_PHOTO",imageStem+".svg","image/svg+xml"));}else{photo.setVerificationStatus("VERIFIED");photo.setAdminNote("Fictional demo vehicle illustration — not a real vehicle photograph.");}
        UploadedAsset rc=v.getDocuments().stream().filter(d->"RC".equals(d.getCategory())).findFirst().orElse(null);
        if(rc==null||rc.getPublicId()==null||!rc.getPublicId().startsWith("demo:")){v.getDocuments().removeIf(d->"RC".equals(d.getCategory()));v.getDocuments().add(asset("RC",imageStem+"-rc-sample.pdf","application/pdf"));}else{rc.setVerificationStatus("VERIFIED");rc.setAdminNote("Fictional demo sample — not a real vehicle registration certificate.");}
        UploadedAsset insurance=v.getDocuments().stream().filter(d->"INSURANCE".equals(d.getCategory())).findFirst().orElse(null);
        if(insurance==null||insurance.getPublicId()==null||!insurance.getPublicId().startsWith("demo:")){v.getDocuments().removeIf(d->"INSURANCE".equals(d.getCategory()));v.getDocuments().add(asset("INSURANCE",imageStem+"-insurance-sample.pdf","application/pdf"));}else{insurance.setVerificationStatus("VERIFIED");insurance.setAdminNote("Fictional demo sample — not an insurance policy.");}
        vehicles.save(v);
    }
    private UploadedAsset asset(String category,String filename,String mime){
        UploadedAsset a=new UploadedAsset();a.setCategory(category);a.setOriginalFilename(filename);a.setPublicId("demo:/demo-documents/"+filename);a.setSecureUrl("/demo-documents/"+filename);a.setResourceType(mime.startsWith("image/")?"image":"raw");a.setDeliveryType("upload");a.setContentType(mime);a.setVerificationStatus("VERIFIED");a.setAdminNote("Fictional demo sample — not a real identity, registration, licence or insurance document.");return a;
    }
    private String key(ServiceCity c,String pin){return c.getId()+"::"+pin;}
    private String route(ServiceCity from,ServiceCity to){return from.getId()+">"+to.getId();}
}
