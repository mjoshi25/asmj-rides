package in.asmj.ride.service;

import in.asmj.ride.model.*;
import in.asmj.ride.repo.DocumentRequirementConfigRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class DocumentRequirementService {
    public static final List<String> ROLES = List.of("DRIVER","FLEET_OWNER","CUSTOMER","VEHICLE");
    private static final Map<String,List<DocumentRequirementConfig.DocumentDefinition>> DEFAULTS = defaults();
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{2,39}");
    private final DocumentRequirementConfigRepository repository;
    public DocumentRequirementService(DocumentRequirementConfigRepository repository){this.repository=repository;}

    private static Map<String,List<DocumentRequirementConfig.DocumentDefinition>> defaults(){
        Map<String,List<DocumentRequirementConfig.DocumentDefinition>> m=new LinkedHashMap<>();
        m.put("DRIVER",new ArrayList<>(List.of(def("IDENTITY","Identity proof","DRIVER",true),def("DRIVING_LICENCE","Driving licence","DRIVER",true),def("ADDRESS_PROOF","Address proof","DRIVER",false))));
        m.put("FLEET_OWNER",new ArrayList<>(List.of(def("IDENTITY","Identity proof","FLEET_OWNER",true),def("BUSINESS_REGISTRATION","Business registration","FLEET_OWNER",true),def("ADDRESS_PROOF","Address proof","FLEET_OWNER",false),def("DRIVING_LICENCE","Driving licence","FLEET_OWNER",false))));
        m.put("CUSTOMER",new ArrayList<>(List.of(def("IDENTITY","Identity proof","CUSTOMER",false),def("ADDRESS_PROOF","Address proof","CUSTOMER",false))));
        m.put("VEHICLE",new ArrayList<>(List.of(def("RC","Registration Certificate (RC)","VEHICLE",true),def("INSURANCE","Insurance","VEHICLE",true),def("PERMIT","Permit","VEHICLE",false),def("FITNESS","Fitness certificate","VEHICLE",false),def("PUC","Pollution certificate (PUC)","VEHICLE",false))));
        return m;
    }
    private static DocumentRequirementConfig.DocumentDefinition def(String c,String n,String r,boolean mandatory){return new DocumentRequirementConfig.DocumentDefinition(c,n,r,mandatory);}
    private DocumentRequirementConfig config(){
        DocumentRequirementConfig c=repository.findById("default").orElseGet(DocumentRequirementConfig::new);
        if(c.getDefinitions()==null||c.getDefinitions().isEmpty()){
            List<DocumentRequirementConfig.DocumentDefinition> migrated=flattenDefaults();
            Map<String,List<String>> legacy=c.getMandatoryByRole()==null?Map.of():c.getMandatoryByRole();
            for(var d:migrated){List<String> configured=legacy.get(d.getRole());if(configured!=null)d.setMandatory(configured.contains(d.getCode()));}
            c.setDefinitions(migrated);c.setId("default");c=repository.save(c);
        }
        return c;
    }
    private List<DocumentRequirementConfig.DocumentDefinition> flattenDefaults(){List<DocumentRequirementConfig.DocumentDefinition> all=new ArrayList<>();DEFAULTS.values().forEach(all::addAll);return all;}
    public synchronized List<DocumentRequirementConfig.DocumentDefinition> definitions(String role){return config().getDefinitions().stream().filter(d->role.equals(d.getRole())).sorted(Comparator.comparing(DocumentRequirementConfig.DocumentDefinition::getName,String.CASE_INSENSITIVE_ORDER)).toList();}
    public synchronized List<String> auditLog(){return new ArrayList<>(config().getAuditLog());}
    public synchronized Map<String,List<DocumentRequirementConfig.DocumentDefinition>> allDefinitions(){Map<String,List<DocumentRequirementConfig.DocumentDefinition>> m=new LinkedHashMap<>();for(String role:ROLES)m.put(role,definitions(role));return m;}
    public synchronized DocumentRequirementConfig.DocumentDefinition definition(String role,String code){return definitions(role).stream().filter(d->code.equals(d.getCode())&&d.isActive()).findFirst().orElse(null);}
    public synchronized List<String> allowedForUpload(String role){return definitions(role).stream().filter(DocumentRequirementConfig.DocumentDefinition::isActive).map(DocumentRequirementConfig.DocumentDefinition::getCode).toList();}
    public synchronized Map<String,List<String>> current(){Map<String,List<String>> result=new LinkedHashMap<>();for(String role:ROLES)result.put(role,definitions(role).stream().filter(DocumentRequirementConfig.DocumentDefinition::isMandatory).map(DocumentRequirementConfig.DocumentDefinition::getCode).toList());return result;}
    public synchronized List<String> mandatory(String role){return current().getOrDefault(role,List.of());}
    public synchronized boolean verifiedUserDocuments(UserAccount user){if(user==null)return false;for(String type:mandatory(user.getRole()))if(user.getDocuments()==null||user.getDocuments().stream().noneMatch(d->type.equals(d.getCategory())&&validVerifiedAsset(user.getRole(),type,d)))return false;return true;}
    public synchronized boolean verifiedVehicleDocuments(Vehicle vehicle){if(vehicle==null||vehicle.getPhotos()==null||vehicle.getPhotos().stream().noneMatch(d->"VERIFIED".equals(d.getVerificationStatus())))return false;for(String type:mandatory("VEHICLE"))if(vehicle.getDocuments()==null||vehicle.getDocuments().stream().noneMatch(d->type.equals(d.getCategory())&&validVerifiedAsset("VEHICLE",type,d)))return false;return true;}
    private boolean validVerifiedAsset(String role,String code,UploadedAsset asset){if(!"VERIFIED".equals(asset.getVerificationStatus()))return false;DocumentRequirementConfig.DocumentDefinition d=definition(role,code);if(d!=null&&d.isExpiryDateRequired()&&(asset.getExpiryDate()==null||asset.getExpiryDate().isBlank()))return false;if(asset.getExpiryDate()!=null&&!asset.getExpiryDate().isBlank())try{return !java.time.LocalDate.parse(asset.getExpiryDate()).isBefore(java.time.LocalDate.now());}catch(java.time.DateTimeException ex){return false;}return true;}
    private DocumentRequirementConfig saveConfig(DocumentRequirementConfig c,String admin){Instant now=Instant.now();Map<String,List<String>> mandatory=new LinkedHashMap<>();for(String role:ROLES)mandatory.put(role,c.getDefinitions().stream().filter(d->role.equals(d.getRole())&&d.isMandatory()).map(DocumentRequirementConfig.DocumentDefinition::getCode).toList());c.setMandatoryByRole(mandatory);c.setUpdatedBy(admin);c.setUpdatedAt(now);if(c.getAuditLog()==null)c.setAuditLog(new ArrayList<>());c.getAuditLog().add(0,now+" | "+admin+" | document requirements changed");if(c.getAuditLog().size()>100)c.setAuditLog(new ArrayList<>(c.getAuditLog().subList(0,100)));return repository.save(c);}
    public synchronized void saveRole(String role,List<String> selected,String adminEmail){if(!ROLES.contains(role))throw new IllegalArgumentException("Unsupported document role.");Set<String> active=new HashSet<>(allowedForUpload(role));LinkedHashSet<String> clean=new LinkedHashSet<>();if(selected!=null)for(String s:selected){if(!active.contains(s))throw new IllegalArgumentException("Unsupported document type for "+role+": "+s);clean.add(s);}DocumentRequirementConfig c=config();for(var d:c.getDefinitions())if(role.equals(d.getRole()))d.setMandatory(clean.contains(d.getCode()));saveConfig(c,adminEmail);}
    public synchronized void add(String role,String code,String name,String description,boolean mandatory,boolean expiry,String admin){
        validateRole(role);String normalized=normalizeCode(code);if(!CODE.matcher(normalized).matches())throw new IllegalArgumentException("Document code must use 3–40 uppercase letters, numbers or underscores and start with a letter.");
        DocumentRequirementConfig c=config();if(c.getDefinitions().stream().anyMatch(d->role.equals(d.getRole())&&normalized.equals(d.getCode())))throw new IllegalArgumentException("Document code already exists for this account type.");
        DocumentRequirementConfig.DocumentDefinition d=new DocumentRequirementConfig.DocumentDefinition();d.setCode(normalized);d.setName(cleanName(name));d.setRole(role);d.setDescription(cleanDescription(description));d.setMandatory(mandatory);d.setExpiryDateRequired(expiry);d.setActive(true);d.setSystemDefined(false);c.getDefinitions().add(d);saveConfig(c,admin);
    }
    public synchronized void update(String role,String code,String name,String description,boolean mandatory,boolean expiry,String admin){DocumentRequirementConfig c=config();var d=find(c,role,code);d.setName(cleanName(name));d.setDescription(cleanDescription(description));if(mandatory&&!d.isActive())throw new IllegalArgumentException("Enable this document before making it mandatory.");d.setMandatory(mandatory);d.setExpiryDateRequired(expiry);saveConfig(c,admin);}
    public synchronized void setActive(String role,String code,boolean active,String admin){DocumentRequirementConfig c=config();var d=find(c,role,code);if(!active&&d.isMandatory())throw new IllegalArgumentException("Make this document optional before disabling it.");d.setActive(active);saveConfig(c,admin);}
    public synchronized void delete(String role,String code,String admin){DocumentRequirementConfig c=config();var d=find(c,role,code);if(d.isSystemDefined())throw new IllegalArgumentException("Built-in document types cannot be permanently deleted. Disable them instead.");if(d.isMandatory())throw new IllegalArgumentException("Make this document optional before deleting it.");c.getDefinitions().remove(d);saveConfig(c,admin);}
    private DocumentRequirementConfig.DocumentDefinition find(DocumentRequirementConfig c,String role,String code){return c.getDefinitions().stream().filter(d->role.equals(d.getRole())&&code.equals(d.getCode())).findFirst().orElseThrow(()->new IllegalArgumentException("Document type not found."));}
    private void validateRole(String role){if(!ROLES.contains(role))throw new IllegalArgumentException("Unsupported account type.");}
    private String normalizeCode(String code){return code==null?"":code.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+","_").replaceAll("^_+|_+$","");}
    private String cleanName(String s){if(s==null||s.trim().isEmpty()||s.trim().length()>80)throw new IllegalArgumentException("Enter a document name (maximum 80 characters).");return s.trim();}
    private String cleanDescription(String s){if(s==null)return "";if(s.length()>240)throw new IllegalArgumentException("Description is too long.");return s.trim();}
}
