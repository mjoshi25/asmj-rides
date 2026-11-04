package in.asmj.ride.web;

import in.asmj.ride.model.*;
import in.asmj.ride.repo.*;
import in.asmj.ride.service.CloudinaryUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Controller
public class SupportController {
    private final UserAccountRepository users;
    private final RideBookingRepository rides;
    private final SupportTicketRepository tickets;
    private final RideReviewRepository reviews;
    private final SafetyReportRepository safety;
    private final CloudinaryUploadService uploads;

    public SupportController(UserAccountRepository u, RideBookingRepository r, SupportTicketRepository t,
                             RideReviewRepository v, SafetyReportRepository s, CloudinaryUploadService c) {
        users=u; rides=r; tickets=t; reviews=v; safety=s; uploads=c;
    }

    private UserAccount me(Authentication a) { return users.findByEmailIgnoreCase(a.getName()).orElseThrow(); }

    @GetMapping("/support")
    public String support(Authentication a, Model m) {
        UserAccount u=me(a);
        boolean admin="ADMIN".equals(u.getRole());
        m.addAttribute("tickets", admin ? tickets.findAllByOrderByCreatedAtDesc() : tickets.findByUserIdOrderByCreatedAtDesc(u.getId()));
        m.addAttribute("rides", rides.findAll().stream().filter(r -> u.getId().equals(r.getCustomerId()) || u.getId().equals(r.getAssignedDriverId())).toList());
        m.addAttribute("admin", admin);
        m.addAttribute("mobile", u.getMobile()==null?"":u.getMobile());
        return "support";
    }

    @PostMapping("/support/create")
    public String create(Authentication a, @RequestParam(required=false) String rideId,
                         @RequestParam String mobile, @RequestParam String category,
                         @RequestParam String subject, @RequestParam String message,
                         @RequestParam(value="attachments",required=false) List<MultipartFile> attachments) {
        UserAccount u=me(a);
        String cleanMobile=mobile==null?"":mobile.replaceAll("[\\s-]","");
        if ("ADMIN".equals(u.getRole()) || !cleanMobile.matches("[+]?[0-9]{10,15}") ||
                !List.of("GENERAL","PAYMENT","TRIP","DRIVER","SAFETY","LOST_ITEM","OTHER").contains(category) ||
                subject==null || subject.isBlank() || subject.length()>120 || message==null || message.isBlank() || message.length()>2000)
            return "redirect:/support?error";
        if (rideId!=null && !rideId.isBlank()) {
            RideBooking r=rides.findById(rideId).orElse(null);
            if (r==null || (!u.getId().equals(r.getCustomerId()) && !u.getId().equals(r.getAssignedDriverId()))) return "redirect:/support?error";
        }
        SupportTicket t=new SupportTicket();
        t.setUserId(u.getId()); t.setUserName(u.getName()); t.setUserMobile(cleanMobile);
        t.setRideId(rideId); t.setCategory(category); t.setSubject(subject.trim()); t.setMessage(message.trim());
        try {
            SupportMessage first=new SupportMessage(); first.setSenderRole(u.getRole()); first.setSenderName(u.getName());
            first.setText(message.trim()); first.setAttachments(uploadAttachments(attachments,"asmj-ride/support/"+u.getId(),"SUPPORT_ATTACHMENT"));
            t.addMessage(first);
            tickets.save(t);
            return "redirect:/support?created";
        } catch (RuntimeException ex) { return "redirect:/support?error=upload"; }
    }

    @PostMapping("/support/{id}/message")
    public String userMessage(Authentication a, @PathVariable String id, @RequestParam String message,
                              @RequestParam(value="attachments",required=false) List<MultipartFile> attachments) {
        UserAccount u=me(a); SupportTicket t=tickets.findById(id).orElseThrow();
        if ("ADMIN".equals(u.getRole()) || !u.getId().equals(t.getUserId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return appendMessage(t,u,message,attachments,false,null);
    }

    @PostMapping("/admin/support/{id}/message")
    public String adminMessage(Authentication a, @PathVariable String id, @RequestParam String message,
                               @RequestParam(value="attachments",required=false) List<MultipartFile> attachments,
                               @RequestParam(value="status",required=false) String status) {
        UserAccount u=me(a);
        if (!"ADMIN".equals(u.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        SupportTicket t=tickets.findById(id).orElseThrow();
        if (status!=null && !List.of("OPEN","IN_PROGRESS","RESOLVED","CLOSED").contains(status)) return "redirect:/support?error";
        return appendMessage(t,u,message,attachments,true,status);
    }

    private String appendMessage(SupportTicket t, UserAccount sender, String text, List<MultipartFile> files, boolean fromAdmin, String requestedStatus) {
        boolean hasFiles=files!=null && files.stream().anyMatch(f->f!=null&&!f.isEmpty());
        if ((text==null || text.isBlank()) && !hasFiles) return "redirect:/support?error";
        if (text!=null && text.length()>2000) return "redirect:/support?error";
        try {
            SupportMessage msg=new SupportMessage(); msg.setSenderRole(sender.getRole()); msg.setSenderName(sender.getName());
            msg.setText(text==null?"":text.trim()); msg.setAttachments(uploadAttachments(files,"asmj-ride/support/"+t.getUserId(),"SUPPORT_ATTACHMENT"));
            t.addMessage(msg);
            if (fromAdmin) { t.setAdminReply(msg.getText()); t.setStatus(requestedStatus==null?"IN_PROGRESS":requestedStatus); }
            else t.setStatus("OPEN");
            tickets.save(t);
            return "redirect:/support?updated";
        } catch (RuntimeException ex) { return "redirect:/support?error=upload"; }
    }

    /** Backward-compatible handler for older admin reply forms/bookmarks. */
    @PostMapping("/admin/support/{id}/reply")
    public String reply(Authentication a,@PathVariable String id,@RequestParam String reply,@RequestParam String status) {
        UserAccount u=me(a); if (!"ADMIN".equals(u.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        SupportTicket t=tickets.findById(id).orElseThrow();
        if(reply==null||reply.isBlank()||reply.length()>2000||!List.of("OPEN","IN_PROGRESS","RESOLVED","CLOSED").contains(status))return "redirect:/support?error";
        SupportMessage msg=new SupportMessage(); msg.setSenderRole("ADMIN"); msg.setSenderName(u.getName()); msg.setText(reply.trim());
        t.addMessage(msg); t.setAdminReply(reply.trim()); t.setStatus(status); tickets.save(t); return "redirect:/support?updated";
    }

    @PostMapping("/rides/{id}/review")
    public String review(Authentication a,@PathVariable String id,@RequestParam(required=false,defaultValue="0")int rating,
                         @RequestParam(required=false,defaultValue="0")int driverRating,@RequestParam(required=false)String comment) {
        UserAccount u=me(a); RideBooking r=rides.findById(id).orElseThrow();
        if(!"COMPLETED".equals(r.getStatus())||(!u.getId().equals(r.getCustomerId())&&!u.getId().equals(r.getAssignedDriverId()))||rating<0||rating>5||driverRating<0||driverRating>5)return "redirect:/customer?reviewError";
        RideReview v=reviews.findByRideId(id).orElseGet(RideReview::new); v.setRideId(id); v.setCustomerId(r.getCustomerId()); v.setDriverId(r.getAssignedDriverId());
        if(u.getId().equals(r.getCustomerId()))v.setCustomerRating(rating);else v.setDriverRating(driverRating);
        if(comment!=null&&comment.length()<=1000)v.setComment(comment.trim()); reviews.save(v); return "redirect:/customer?reviewed";
    }

    @PostMapping("/rides/{id}/sos")
    public String sos(Authentication a,@PathVariable String id,@RequestParam(required=false)String details,
                       @RequestParam(value="attachments",required=false)List<MultipartFile> attachments) {
        UserAccount u=me(a); RideBooking r=rides.findById(id).orElseThrow();
        if(!u.getId().equals(r.getCustomerId())&&!u.getId().equals(r.getAssignedDriverId()))return "redirect:/customer?error";
        SafetyReport s=new SafetyReport(); s.setRideId(id); s.setUserId(u.getId()); s.setUserName(u.getName());
        s.setType("SOS"); s.setDetails(details==null||details.isBlank()?"Emergency assistance requested":details.substring(0,Math.min(1000,details.length())));
        try{s.setAttachments(uploadAttachments(attachments,"asmj-ride/safety-reports/"+u.getId(),"SAFETY_EVIDENCE"));safety.save(s);return "redirect:/support?sosSent";}
        catch(RuntimeException ex){return "redirect:/support?error=upload";}
    }

    @PostMapping("/rides/{id}/complaint")
    public String complaint(Authentication a,@PathVariable String id,@RequestParam String type,@RequestParam String details,
                            @RequestParam(value="attachments",required=false)List<MultipartFile> attachments) {
        UserAccount u=me(a); RideBooking r=rides.findById(id).orElseThrow();
        if((!u.getId().equals(r.getCustomerId())&&!u.getId().equals(r.getAssignedDriverId()))||details==null||details.isBlank()||details.length()>1500)return "redirect:/support?error";
        SafetyReport s=new SafetyReport();s.setRideId(id);s.setUserId(u.getId());s.setUserName(u.getName());
        s.setType(List.of("LOST_ITEM","SAFETY","FRAUD","OTHER").contains(type)?type:"OTHER");s.setDetails(details.trim());
        try{s.setAttachments(uploadAttachments(attachments,"asmj-ride/safety-reports/"+u.getId(),"SAFETY_EVIDENCE"));safety.save(s);return "redirect:/support?reported";}
        catch(RuntimeException ex){return "redirect:/support?error=upload";}
    }

    @PostMapping("/admin/safety/{id}")
    public String safetyUpdate(@PathVariable String id,@RequestParam String status,@RequestParam(required=false)String note) {
        SafetyReport s=safety.findById(id).orElseThrow();if(!List.of("OPEN","IN_PROGRESS","RESOLVED","CLOSED").contains(status))return "redirect:/support?error";
        s.setStatus(status);s.setAdminNote(note);safety.save(s);return "redirect:/support?updated";
    }

    private List<UploadedAsset> uploadAttachments(List<MultipartFile> files,String folder,String category) {
        List<UploadedAsset> result=new ArrayList<>(); if(files==null)return result;
        try { for(MultipartFile f:files) if(f!=null&&!f.isEmpty()) {
            if(result.size()>=5)throw new IllegalArgumentException("Maximum 5 attachments allowed.");
            result.add(uploads.upload(f,folder,category,true));
        } return result; }
        catch(RuntimeException ex){result.forEach(uploads::delete);throw ex;}
    }
}
