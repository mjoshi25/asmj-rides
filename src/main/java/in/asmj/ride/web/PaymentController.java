package in.asmj.ride.web;

import in.asmj.ride.model.*;
import in.asmj.ride.repo.*;
import in.asmj.ride.service.CloudinaryUploadService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Controller
public class PaymentController {
 private final UserAccountRepository users; private final RideBookingRepository rides; private final PaymentRecordRepository payments; private final FareRuleRepository fares; private final CloudinaryUploadService uploads;
 public PaymentController(UserAccountRepository u,RideBookingRepository r,PaymentRecordRepository p,FareRuleRepository f,CloudinaryUploadService c){users=u;rides=r;payments=p;fares=f;uploads=c;}
 private UserAccount me(Authentication a){return users.findByEmailIgnoreCase(a.getName()).orElseThrow();}
 @GetMapping("/payments") public String payments(Authentication a,Model m){UserAccount u=me(a);List<PaymentRecord> list=payments.findAllByOrderByCreatedAtDesc();
  if("DRIVER".equals(u.getRole()))list=list.stream().filter(x->u.getId().equals(x.getDriverId())||u.getId().equals(x.getReturnDriverId())).toList();
  else if("FLEET_OWNER".equals(u.getRole()))list=list.stream().filter(x->u.getId().equals(x.getFleetOwnerId())||u.getId().equals(x.getReturnFleetOwnerId())).toList();
  else if("CUSTOMER".equals(u.getRole()))list=list.stream().filter(x->u.getId().equals(x.getCustomerId())).toList();
  m.addAttribute("payments",list);m.addAttribute("role",u.getRole());m.addAttribute("viewerId",u.getId());return "payments";}
 @PostMapping("/payments/ride/{rideId}/submit") public String submit(Authentication a,@PathVariable String rideId,@RequestParam String method,@RequestParam(required=false) String upiReference,@RequestParam(required=false) MultipartFile proof){
  UserAccount u=me(a);RideBooking r=rides.findById(rideId).orElseThrow();
  boolean journeyComplete="COMPLETED".equals(r.getStatus()) && (!"ROUND_TRIP".equals(r.getJourneyType()) || "COMPLETED".equals(r.getReturnLegStatus()));
  if(!u.getId().equals(r.getCustomerId())||!journeyComplete||!List.of("CASH","UPI").contains(method)||!method.equals(r.getPaymentMethod()))return "redirect:/customer?paymentError";
  if("UPI".equals(method)&&(upiReference==null||!upiReference.matches("[A-Za-z0-9@._-]{6,80}")||proof==null||proof.isEmpty()))return "redirect:/customer?paymentError";
  PaymentRecord p=payments.findByRideId(rideId).orElseGet(PaymentRecord::new);if(p.getId()!=null&&!List.of("PAYMENT_REJECTED").contains(p.getStatus()))return "redirect:/payments?alreadySubmitted";
  p.setRideId(rideId);p.setCustomerId(u.getId());p.setDriverId(r.getAssignedDriverId());
  UserAccount outwardDriver=r.getAssignedDriverId()==null?null:users.findById(r.getAssignedDriverId()).orElse(null);
  p.setFleetOwnerId(outwardDriver==null?null:outwardDriver.getFleetOwnerId());
  double outwardGross="ROUND_TRIP".equals(r.getJourneyType())?Math.max(0,r.getFinalFare()-r.getReturnFinalFare()):r.getFinalFare();
  p.setOutwardGrossAmount(outwardGross);p.setGrossAmount(r.getFinalFare());
  double outwardRate=outwardDriver!=null&&outwardDriver.getFleetOwnerId()!=null?fare().getFleetCommissionPercent():fare().getIndividualCommissionPercent();
  p.setCommissionRate(outwardRate);p.setCommissionAmount(Math.round(outwardGross*outwardRate)/100.0);p.setDriverNetAmount(Math.max(0,outwardGross-p.getCommissionAmount()));
  if("ROUND_TRIP".equals(r.getJourneyType())){
   p.setReturnDriverId(r.getReturnAssignedDriverId());UserAccount returnDriver=r.getReturnAssignedDriverId()==null?null:users.findById(r.getReturnAssignedDriverId()).orElse(null);
   p.setReturnFleetOwnerId(returnDriver==null?null:returnDriver.getFleetOwnerId());double returnGross=r.getReturnFinalFare();p.setReturnGrossAmount(returnGross);
   double returnRate=returnDriver!=null&&returnDriver.getFleetOwnerId()!=null?fare().getFleetCommissionPercent():fare().getIndividualCommissionPercent();p.setReturnCommissionRate(returnRate);p.setReturnCommissionAmount(Math.round(returnGross*returnRate)/100.0);p.setReturnDriverNetAmount(Math.max(0,returnGross-p.getReturnCommissionAmount()));
  } else {p.setReturnGrossAmount(0);p.setReturnCommissionAmount(0);p.setReturnDriverNetAmount(0);}
  p.setMethod(method);p.setUpiReference("UPI".equals(method)?upiReference.trim():null);
  if("UPI".equals(method)){try{p.setPaymentProof(uploads.upload(proof,"asmj-ride/payment-proofs/"+rideId,"UPI_PAYMENT_PROOF",true));}catch(RuntimeException ex){return "redirect:/customer?paymentError";}}
  String nextStatus="CASH".equals(method)?"CASH_TO_COLLECT":"UPI_SUBMITTED";p.addStatusEvent(p.getStatus(),nextStatus,u.getName(),"Customer submitted payment details");p.setStatus(nextStatus);payments.save(p);return "redirect:/payments?submitted";
 }
 @PostMapping("/payments/{id}/cash-collected") public String cashCollected(Authentication a,@PathVariable String id){UserAccount u=me(a);PaymentRecord p=payments.findById(id).orElseThrow();RideBooking r=rides.findById(p.getRideId()).orElse(null);boolean assigned=r!=null&&(u.getId().equals(r.getAssignedDriverId())||u.getId().equals(r.getReturnAssignedDriverId()));boolean complete=r!=null&&"COMPLETED".equals(r.getStatus())&&(!"ROUND_TRIP".equals(r.getJourneyType())||"COMPLETED".equals(r.getReturnLegStatus()));if(!assigned||!complete||!"DRIVER".equals(u.getRole())||!"CASH".equals(p.getMethod())||!"CASH_TO_COLLECT".equals(p.getStatus()))return "redirect:/payments?error";p.addStatusEvent(p.getStatus(),"PAID",u.getName(),"Driver confirmed cash collection");p.setStatus("PAID");payments.save(p);return "redirect:/payments?updated";}
 @PostMapping("/admin/payments/{id}/verify") public String verify(@PathVariable String id,@RequestParam String decision,@RequestParam(required=false)String note){PaymentRecord p=payments.findById(id).orElseThrow();String oldStatus=p.getStatus();if("APPROVE".equals(decision)&&"UPI_SUBMITTED".equals(p.getStatus())){p.setStatus("PAID");if(p.getPaymentProof()!=null)p.getPaymentProof().setVerificationStatus("VERIFIED");}else if("REJECT".equals(decision)&&"UPI_SUBMITTED".equals(p.getStatus())){p.setStatus("PAYMENT_REJECTED");if(p.getPaymentProof()!=null)p.getPaymentProof().setVerificationStatus("REJECTED");}else if("REFUND".equals(decision)&&"PAID".equals(p.getStatus())&&!p.isSettled()&&!p.isReturnSettled())p.setStatus("REFUND_PROCESSED");else return "redirect:/payments?error";p.addStatusEvent(oldStatus,p.getStatus(),"ADMIN",note==null?"Admin reviewed payment":note);p.setAdminNote(note);payments.save(p);return "redirect:/payments?updated";}
 @PostMapping("/admin/payments/{id}/cash-confirm") public String adminCashConfirm(@PathVariable String id,@RequestParam(required=false) String note){PaymentRecord p=payments.findById(id).orElseThrow();if(!"CASH".equals(p.getMethod())||!"CASH_TO_COLLECT".equals(p.getStatus()))return "redirect:/payments?error";p.addStatusEvent(p.getStatus(),"PAID","ADMIN",note==null||note.isBlank()?"Admin confirmed cash collection":note.trim());p.setStatus("PAID");p.setAdminNote(note);payments.save(p);return "redirect:/payments?updated";}
 @PostMapping("/admin/payments/{id}/settle-leg") public String settleLeg(@PathVariable String id,@RequestParam String leg){PaymentRecord p=payments.findById(id).orElseThrow();if(!"PAID".equals(p.getStatus()))return "redirect:/payments?error";if("OUTWARD".equals(leg)){if(p.isSettled()||p.getDriverId()==null)return "redirect:/payments?error";p.setSettled(true);p.addStatusEvent("PAID","PAID","ADMIN","Outward driver allocation marked settled");}else if("RETURN".equals(leg)){if(p.isReturnSettled()||p.getReturnDriverId()==null)return "redirect:/payments?error";p.setReturnSettled(true);p.addStatusEvent("PAID","PAID","ADMIN","Return driver allocation marked settled");}else return "redirect:/payments?error";payments.save(p);return "redirect:/payments?settled";}
 @PostMapping("/admin/payments/{id}/settle") public String settle(@PathVariable String id){PaymentRecord p=payments.findById(id).orElseThrow();if(!"PAID".equals(p.getStatus()))return "redirect:/payments?error";if(!p.isSettled()&&p.getDriverId()!=null)p.setSettled(true);if(!p.isReturnSettled()&&p.getReturnDriverId()!=null)p.setReturnSettled(true);payments.save(p);return "redirect:/payments?settled";}
 private FareRule fare(){return fares.findById("default").orElseGet(()->fares.save(new FareRule()));}
}
