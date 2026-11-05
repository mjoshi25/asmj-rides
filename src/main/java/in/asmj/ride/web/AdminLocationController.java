package in.asmj.ride.web;

import in.asmj.ride.model.ServiceCity;
import in.asmj.ride.model.RouteDistance;
import in.asmj.ride.repo.ServiceCityRepository;
import in.asmj.ride.repo.RouteDistanceRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller
public class AdminLocationController {
 private final ServiceCityRepository cities;
 private final RouteDistanceRepository routes;
 public AdminLocationController(ServiceCityRepository c,RouteDistanceRepository r){cities=c;routes=r;}

 @GetMapping("/admin/locations") String page(Model m){
  m.addAttribute("cities",cities.findAllByOrderByCityAsc());
  m.addAttribute("routeDistances",routes.findAll());
  return "admin-locations";
 }

 @PostMapping("/admin/locations/save") String save(@RequestParam(required=false) String id,@RequestParam String city,@RequestParam String district,@RequestParam String state,@RequestParam(required=false) String pinCodes,@RequestParam(defaultValue="true") boolean active){
  if(city.isBlank()||district.isBlank()||state.isBlank()||city.length()>80||district.length()>80||state.length()>80)return "redirect:/admin/locations?error=details";
  List<String> pins=Arrays.stream((pinCodes==null?"":pinCodes).split("[,\\s]+" )).map(String::trim).filter(x->!x.isBlank()).distinct().toList();
  if(pins.isEmpty()||pins.stream().anyMatch(x->!x.matches("[0-9]{6}")))return "redirect:/admin/locations?error=pins";
  ServiceCity c=id==null||id.isBlank()?new ServiceCity():cities.findById(id).orElseThrow();
  c.setCity(city.trim());c.setDistrict(district.trim());c.setState(state.trim());c.setPinCodes(new ArrayList<>(pins));c.setActive(active);c.touch();cities.save(c);
  return "redirect:/admin/locations?saved";
 }

 @PostMapping("/admin/locations/{id}/toggle") String toggle(@PathVariable String id){ServiceCity c=cities.findById(id).orElseThrow();c.setActive(!c.isActive());c.touch();cities.save(c);return "redirect:/admin/locations";}

 @PostMapping("/admin/routes/save") String saveRoute(@RequestParam(required=false) String id,@RequestParam String sourceCityId,@RequestParam(required=false,defaultValue="") String sourcePinCode,@RequestParam String destinationCityId,@RequestParam(required=false,defaultValue="") String destinationPinCode,@RequestParam double distanceKm,@RequestParam(defaultValue="false") boolean active){
  ServiceCity from=cities.findById(sourceCityId).filter(ServiceCity::isActive).orElse(null),to=cities.findById(destinationCityId).filter(ServiceCity::isActive).orElse(null);
  if(from==null||to==null||sourcePinCode.isBlank()!=destinationPinCode.isBlank()||from.getId().equals(to.getId())&&sourcePinCode.equals(destinationPinCode)||!Double.isFinite(distanceKm)||distanceKm<1||distanceKm>3000)return "redirect:/admin/locations?routeError";
  if(!sourcePinCode.isBlank()&&!from.getPinCodes().contains(sourcePinCode)||!destinationPinCode.isBlank()&&!to.getPinCodes().contains(destinationPinCode))return "redirect:/admin/locations?routeError";
  RouteDistance r=id==null||id.isBlank()?new RouteDistance():routes.findById(id).orElseThrow();
  r.setSourceCityId(from.getId());r.setSourceCity(from.getCity());r.setSourcePinCode(sourcePinCode);
  r.setDestinationCityId(to.getId());r.setDestinationCity(to.getCity());r.setDestinationPinCode(destinationPinCode);
  r.setDistanceKm(distanceKm);r.setActive(active);r.touch();routes.save(r);
  return "redirect:/admin/locations?routeSaved";
 }

 @PostMapping("/admin/routes/{id}/delete") String deleteRoute(@PathVariable String id){routes.deleteById(id);return "redirect:/admin/locations?routeDeleted";}
}
