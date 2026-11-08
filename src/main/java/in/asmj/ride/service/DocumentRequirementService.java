package in.asmj.ride.service;

import in.asmj.ride.model.*;
import in.asmj.ride.repo.DocumentRequirementConfigRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service
public class DocumentRequirementService {
    public static final Map<String,List<String>> CATALOG = Map.of(
        "DRIVER", List.of("IDENTITY","DRIVING_LICENCE","ADDRESS_PROOF"),
        "FLEET_OWNER", List.of("IDENTITY","BUSINESS_REGISTRATION","ADDRESS_PROOF","DRIVING_LICENCE"),
        "CUSTOMER", List.of("IDENTITY","ADDRESS_PROOF"),
        "VEHICLE", List.of("RC","INSURANCE","PERMIT","FITNESS","PUC")
    );
    private final DocumentRequirementConfigRepository repository;
    public DocumentRequirementService(DocumentRequirementConfigRepository repository){this.repository=repository;}
    private Map<String,List<String>> defaults(){Map<String,List<String>> m=new LinkedHashMap<>();m.put("DRIVER",List.of("IDENTITY","DRIVING_LICENCE"));m.put("FLEET_OWNER",List.of("IDENTITY","BUSINESS_REGISTRATION"));m.put("CUSTOMER",List.of());m.put("VEHICLE",List.of("RC","INSURANCE"));return m;}
    public Map<String,List<String>> current(){DocumentRequirementConfig c=repository.findById("default").orElse(null);Map<String,List<String>> result=defaults();if(c!=null&&c.getMandatoryByRole()!=null)c.getMandatoryByRole().forEach((k,v)->result.put(k,v==null?List.of():new ArrayList<>(v)));return result;}
    public List<String> mandatory(String role){return current().getOrDefault(role,List.of());}
    public boolean verifiedUserDocuments(UserAccount user){if(user==null)return false;for(String type:mandatory(user.getRole()))if(user.getDocuments()==null||user.getDocuments().stream().noneMatch(d->type.equals(d.getCategory())&&"VERIFIED".equals(d.getVerificationStatus())))return false;return true;}
    public boolean verifiedVehicleDocuments(Vehicle vehicle){if(vehicle==null||vehicle.getPhotos()==null||vehicle.getPhotos().stream().noneMatch(d->"VERIFIED".equals(d.getVerificationStatus())))return false;for(String type:mandatory("VEHICLE"))if(vehicle.getDocuments()==null||vehicle.getDocuments().stream().noneMatch(d->type.equals(d.getCategory())&&"VERIFIED".equals(d.getVerificationStatus())))return false;return true;}
    public void saveRole(String role,List<String> selected,String adminEmail){if(!CATALOG.containsKey(role))throw new IllegalArgumentException("Unsupported document role.");Set<String> allowed=new HashSet<>(CATALOG.get(role));LinkedHashSet<String> clean=new LinkedHashSet<>();if(selected!=null)for(String s:selected){if(!allowed.contains(s))throw new IllegalArgumentException("Unsupported document type for "+role+": "+s);clean.add(s);}DocumentRequirementConfig c=repository.findById("default").orElseGet(DocumentRequirementConfig::new);Map<String,List<String>> map=current();map.put(role,new ArrayList<>(clean));c.setMandatoryByRole(map);c.setUpdatedBy(adminEmail);c.setUpdatedAt(Instant.now());repository.save(c);}
}
