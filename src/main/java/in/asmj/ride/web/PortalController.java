package in.asmj.ride.web;
import in.asmj.ride.model.Vehicle;import in.asmj.ride.repo.*;import org.springframework.stereotype.Controller;import org.springframework.ui.Model;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.Authentication;import java.util.List;
@Controller public class PortalController {
 private final UserAccountRepository users;private final VehicleRepository vehicles;public PortalController(UserAccountRepository u,VehicleRepository v){users=u;vehicles=v;}
 @GetMapping("/partner")String partner(Authentication a,Model m){var u=users.findByEmailIgnoreCase(a.getName()).orElseThrow();m.addAttribute("name",u.getName());m.addAttribute("role",u.getRole());m.addAttribute("vehicles",vehicles.findByOwnerId(u.getId()));return "partner";}
 @PostMapping("/partner/vehicle")String add(Authentication a,@RequestParam String category,@RequestParam String registrationNumber,@RequestParam(required=false)String model){var u=users.findByEmailIgnoreCase(a.getName()).orElseThrow();if(!List.of("CAR","AUTO","BIKE","OTHER").contains(category)||registrationNumber==null||registrationNumber.isBlank()||registrationNumber.length()>20)return "redirect:/partner?error=vehicle";Vehicle v=new Vehicle();v.setOwnerId(u.getId());v.setCategory(category);v.setRegistrationNumber(registrationNumber.toUpperCase());v.setModel(model);vehicles.save(v);return "redirect:/partner";}

 @PostMapping("/partner/vehicle/{id}/assign")String assignDriver(Authentication a,@PathVariable String id,@RequestParam String driverEmail){
  var owner=users.findByEmailIgnoreCase(a.getName()).orElseThrow();if(!"FLEET_OWNER".equals(owner.getRole()))return "redirect:/partner";
  var driver=users.findByEmailIgnoreCase(driverEmail).orElse(null);var v=vehicles.findById(id).orElseThrow();
  if(driver==null||!"DRIVER".equals(driver.getRole())||!"APPROVED".equals(driver.getStatus())||!driver.isMobileVerified()||!owner.getId().equals(v.getOwnerId()))return "redirect:/partner?error=driver";
  v.setAssignedDriverId(driver.getId());vehicles.save(v);driver.setFleetOwnerId(owner.getId());users.save(driver);return "redirect:/partner?assigned";
 }
 @GetMapping("/admin")String admin(Model m){m.addAttribute("partners",users.findByRoleIn(List.of("DRIVER","FLEET_OWNER")));m.addAttribute("vehicles",vehicles.findAll());return "admin";}
 @PostMapping("/admin/partner/{id}")String approve(@PathVariable String id,@RequestParam String status){var u=users.findById(id).orElseThrow();if(!List.of("APPROVED","REJECTED","PENDING","SUSPENDED").contains(status))throw new IllegalArgumentException();u.setStatus(status);users.save(u);return "redirect:/admin";}
 @PostMapping("/admin/vehicle/{id}")String vehicle(@PathVariable String id,@RequestParam String status){var v=vehicles.findById(id).orElseThrow();if(!List.of("APPROVED","REJECTED","PENDING").contains(status))throw new IllegalArgumentException();v.setStatus(status);vehicles.save(v);return "redirect:/admin";}
}