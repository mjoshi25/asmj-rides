package in.asmj.ride.web;

import in.asmj.ride.model.UserAccount;
import in.asmj.ride.repo.RideNotificationRepository;
import in.asmj.ride.repo.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class NotificationController {
    private final UserAccountRepository users;
    private final RideNotificationRepository notifications;
    public NotificationController(UserAccountRepository u, RideNotificationRepository n){users=u;notifications=n;}
    private UserAccount me(Authentication a){return users.findByEmailIgnoreCase(a.getName()).orElseThrow();}

    @GetMapping("/notifications")
    public String page(Authentication a, Model m){
        UserAccount u=me(a);
        m.addAttribute("notifications",notifications.findByRecipientIdOrderByCreatedAtDesc(u.getId()));
        m.addAttribute("unreadCount",notifications.findByRecipientIdOrderByCreatedAtDesc(u.getId()).stream().filter(n->!n.isRead()).count());
        return "notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markRead(Authentication a,@PathVariable String id){
        UserAccount u=me(a);
        notifications.findById(id).filter(n->u.getId().equals(n.getRecipientId())).ifPresent(n->{n.setRead(true);notifications.save(n);});
        return "redirect:/notifications";
    }
}
