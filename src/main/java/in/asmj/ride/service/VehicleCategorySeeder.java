package in.asmj.ride.service;

import in.asmj.ride.model.VehicleCategory;
import in.asmj.ride.repo.VehicleCategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

/** Adds initial category records without overwriting administrator edits on subsequent startups. */
@Component
public class VehicleCategorySeeder implements CommandLineRunner {
    private final VehicleCategoryRepository categories;
    public VehicleCategorySeeder(VehicleCategoryRepository categories){this.categories=categories;}

    @Override public void run(String... args){
        seed("CAR","Cars",null,"Passenger cars",10,List.of("WITH_DRIVER","SELF_DRIVE"));
        seed("AUTO","Three-wheelers",null,"Auto-rickshaws and passenger three-wheelers",20,List.of("WITH_DRIVER"));
        seed("BIKE","Two-wheelers",null,"Motorcycles and scooters",30,List.of("WITH_DRIVER","SELF_DRIVE"));
        seed("VAN","Passenger vans",null,"Vans and people carriers",40,List.of("WITH_DRIVER"));
        seed("BUS","Buses",null,"Mini, school and tourist buses",50,List.of("WITH_DRIVER"));
        seed("GOODS","Goods vehicles",null,"Pickup vehicles, mini trucks and trucks",60,List.of("WITH_DRIVER"));
        seed("HEAVY_COMMERCIAL","Heavy commercial",null,"Heavy commercial and freight vehicles",70,List.of("WITH_DRIVER"));
        seed("SPECIAL","Special vehicles",null,"Ambulance, tractor, crane and specialist vehicles",80,List.of("WITH_DRIVER"));
        seed("HATCHBACK","Hatchback","CAR","Compact hatchback cars",11,List.of("WITH_DRIVER","SELF_DRIVE"));
        seed("SEDAN","Sedan","CAR","Sedan cars",12,List.of("WITH_DRIVER","SELF_DRIVE"));
        seed("SUV","SUV","CAR","Sport utility vehicles",13,List.of("WITH_DRIVER","SELF_DRIVE"));
        seed("MUV_MPV","MUV / MPV","CAR","Multi-utility and people mover vehicles",14,List.of("WITH_DRIVER","SELF_DRIVE"));
        seed("LUXURY","Luxury cars","CAR","Premium and luxury cars",15,List.of("WITH_DRIVER","SELF_DRIVE"));
        seed("SCOOTER","Scooter","BIKE","Scooters",31,List.of("WITH_DRIVER","SELF_DRIVE"));
        seed("MOTORCYCLE","Motorcycle","BIKE","Motorcycles",32,List.of("WITH_DRIVER","SELF_DRIVE"));
        seed("TEMPO_TRAVELLER","Tempo Traveller","VAN","Passenger tempo travellers",41,List.of("WITH_DRIVER"));
        seed("MINIBUS","Minibus","BUS","Minibuses",51,List.of("WITH_DRIVER"));
        seed("TOURIST_BUS","Tourist bus","BUS","Tourist and charter buses",52,List.of("WITH_DRIVER"));
        seed("PICKUP","Pickup / mini truck","GOODS","Pickup and mini trucks",61,List.of("WITH_DRIVER"));
        seed("TRUCK","Truck","GOODS","Commercial trucks",62,List.of("WITH_DRIVER"));
    }

    private void seed(String code,String name,String parent,String description,int order,List<String> modes){
        if(categories.findByCodeIgnoreCase(code).isPresent()) return;
        VehicleCategory c=new VehicleCategory();c.setCode(code);c.setName(name);c.setParentCode(parent);c.setDescription(description);c.setSortOrder(order);c.setSupportedRentalModes(modes);c.setActive(true);
        if(List.of("GOODS","HEAVY_COMMERCIAL","PICKUP","TRUCK").contains(code)) c.setServiceType("GOODS");
        else c.setServiceType("PASSENGER");
        if(List.of("HATCHBACK","SEDAN","SUV","MUV_MPV","LUXURY").contains(code)) c.setPassengerCapacity(4);
        if("AUTO".equals(code)) c.setPassengerCapacity(3);
        if("BIKE".equals(code)||"SCOOTER".equals(code)||"MOTORCYCLE".equals(code)) c.setPassengerCapacity(1);
        categories.save(c);
    }
}
