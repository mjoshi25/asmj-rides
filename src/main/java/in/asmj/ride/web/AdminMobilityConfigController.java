package in.asmj.ride.web;

import in.asmj.ride.model.*;
import in.asmj.ride.repo.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.regex.Pattern;

@Controller
public class AdminMobilityConfigController {
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,39}");
    private final VehicleCategoryRepository categories;
    private final ServiceCityRepository cities;
    private final MobilityPlatformConfigRepository configs;

    public AdminMobilityConfigController(VehicleCategoryRepository categories, ServiceCityRepository cities, MobilityPlatformConfigRepository configs) {
        this.categories=categories; this.cities=cities; this.configs=configs;
    }

    @GetMapping("/admin/mobility-config")
    public String page(Model model) {
        List<VehicleCategory> all=categories.findAll(); all.sort(Comparator.comparingInt(VehicleCategory::getSortOrder).thenComparing(VehicleCategory::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        model.addAttribute("categories",all);
        model.addAttribute("cities",cities.findAllByOrderByCityAsc());
        model.addAttribute("config",configs.findById("default").orElseGet(MobilityPlatformConfig::new));
        return "admin-mobility-config";
    }

    @PostMapping("/admin/mobility-config/category/save")
    public String saveCategory(@RequestParam(required=false) String id, @RequestParam String code, @RequestParam String name,
            @RequestParam String serviceType, @RequestParam(defaultValue="0") int passengerCapacity,
            @RequestParam(defaultValue="0") double maximumPayloadKg, @RequestParam(defaultValue="0") int sortOrder,
            @RequestParam(required=false, defaultValue="") String description, @RequestParam(defaultValue="false") boolean active) {
        String normalized=code==null?"":code.trim().toUpperCase(Locale.ROOT);
        if(!CODE.matcher(normalized).matches() || name==null || name.isBlank() || name.trim().length()>80 ||
           !List.of("PASSENGER","GOODS","BOTH").contains(serviceType) || passengerCapacity<0 || passengerCapacity>1000 ||
           !Double.isFinite(maximumPayloadKg) || maximumPayloadKg<0 || maximumPayloadKg>100000 || sortOrder<0 || sortOrder>10000 || description.length()>500)
            return "redirect:/admin/mobility-config?categoryError";
        Optional<VehicleCategory> duplicate=categories.findByCodeIgnoreCase(normalized);
        if(duplicate.isPresent() && (id==null || !duplicate.get().getId().equals(id))) return "redirect:/admin/mobility-config?duplicateCode";
        VehicleCategory c=id==null||id.isBlank()?new VehicleCategory():categories.findById(id).orElse(null);
        if(c==null)return "redirect:/admin/mobility-config?categoryError";
        c.setCode(normalized); c.setName(name.trim()); c.setServiceType(serviceType); c.setPassengerCapacity(passengerCapacity);
        c.setMaximumPayloadKg(maximumPayloadKg); c.setSortOrder(sortOrder); c.setDescription(description.trim()); c.setActive(active); c.setUpdatedAt(java.time.Instant.now());
        categories.save(c);
        return "redirect:/admin/mobility-config?categorySaved";
    }

    @PostMapping("/admin/mobility-config/settings/save")
    public String saveSettings(@RequestParam(defaultValue="false") boolean passengerServiceEnabled,
            @RequestParam(defaultValue="false") boolean goodsServiceEnabled,
            @RequestParam(defaultValue="10") double defaultCommission,
            @RequestParam Map<String,String> fields) {
        if(!Double.isFinite(defaultCommission)||defaultCommission<0||defaultCommission>50)return "redirect:/admin/mobility-config?settingsError";
        MobilityPlatformConfig c=configs.findById("default").orElseGet(MobilityPlatformConfig::new);
        Map<String,Double> rates=new LinkedHashMap<>();
        for(VehicleCategory category:categories.findAll()) {
            String raw=fields.get("commission_"+category.getCode());
            if(raw==null||raw.isBlank())continue;
            try { double rate=Double.parseDouble(raw); if(!Double.isFinite(rate)||rate<0||rate>50)return "redirect:/admin/mobility-config?settingsError"; rates.put(category.getCode(),rate); }
            catch(NumberFormatException ex){return "redirect:/admin/mobility-config?settingsError";}
        }
        c.setServiceCoverageMode("CITY"); c.setTripAssignmentMode("BOTH"); c.setCommissionPolicy("CATEGORY");
        c.setPassengerServiceEnabled(passengerServiceEnabled); c.setGoodsServiceEnabled(goodsServiceEnabled);
        c.setDefaultCategoryCommissionPercent(defaultCommission); c.setCategoryCommissionPercent(rates); c.touch(); configs.save(c);
        return "redirect:/admin/mobility-config?settingsSaved";
    }

    @PostMapping("/admin/mobility-config/city/{id}/categories")
    public String saveCityCategories(@PathVariable String id,@RequestParam(required=false) List<String> enabledCategoryCodes) {
        ServiceCity city=cities.findById(id).orElse(null); if(city==null)return "redirect:/admin/mobility-config?cityError";
        Set<String> valid=new HashSet<>(); categories.findByActiveTrueOrderBySortOrderAscNameAsc().forEach(c->valid.add(c.getCode()));
        List<String> selected=enabledCategoryCodes==null?List.of():enabledCategoryCodes.stream().filter(valid::contains).distinct().toList();
        city.setEnabledCategoryCodes(selected); city.setCategorySelectionConfigured(true); city.touch(); cities.save(city);
        return "redirect:/admin/mobility-config?citySaved";
    }

    @PostMapping("/admin/mobility-config/category/{id}/toggle")
    public String toggleCategory(@PathVariable String id) {
        VehicleCategory c=categories.findById(id).orElse(null); if(c==null)return "redirect:/admin/mobility-config?categoryError";
        c.setActive(!c.isActive()); c.setUpdatedAt(java.time.Instant.now()); categories.save(c);
        return "redirect:/admin/mobility-config?categorySaved";
    }
}
