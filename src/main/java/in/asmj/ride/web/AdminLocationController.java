package in.asmj.ride.web;
import in.asmj.ride.model.ServiceCity; import in.asmj.ride.repo.ServiceCityRepository;
import org.springframework.stereotype.Controller; import org.springframework.ui.Model; import org.springframework.web.bind.annotation.*; import java.util.*;
@Controller
public class AdminLocationController {
 private final ServiceCityRepository cities; public AdminLocationController(ServiceCityRepository c){cities=c;}
 @GetMapping("/admin/locations") String page(Model m){m.addAttribute("cities",cities.findAllByOrderByCityAsc());return "admin-locations";}
 @PostMapping("/admin/locations/save") String save(@RequestParam(required=false) String id,@RequestParam String city,@RequestParam String district,@RequestParam String state,@RequestParam(required=false) String pinCodes,@RequestParam(defaultValue="true") boolean active){if(city.isBlank()||district.isBlank()||state.isBlank()||city.length()>80||district.length()>80||state.length()>80)return "redirect:/admin/locations?error=details";List<String> pins=Arrays.stream((pinCodes==null?"":pinCodes).split("[,\\s]+" )).map(String::trim).filter(x->!x.isBlank()).distinct().toList();if(pins.isEmpty()||pins.stream().anyMatch(x->!x.matches("[0-9]{6}")))return "redirect:/admin/locations?error=pins";ServiceCity c=id==null||id.isBlank()?new ServiceCity():cities.findById(id).orElseThrow();c.setCity(city.trim());c.setDistrict(district.trim());c.setState(state.trim());c.setPinCodes(new ArrayList<>(pins));c.setActive(active);c.touch();cities.save(c);return "redirect:/admin/locations?saved";}
 @PostMapping("/admin/locations/{id}/toggle") String toggle(@PathVariable String id){ServiceCity c=cities.findById(id).orElseThrow();c.setActive(!c.isActive());c.touch();cities.save(c);return "redirect:/admin/locations";}
}
