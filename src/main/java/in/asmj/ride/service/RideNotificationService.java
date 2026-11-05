package in.asmj.ride.service;

import in.asmj.ride.model.RideNotification;
import in.asmj.ride.model.UserAccount;
import in.asmj.ride.repo.RideNotificationRepository;
import in.asmj.ride.repo.UserAccountRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RideNotificationService {
    private final RideNotificationRepository notifications;
    private final UserAccountRepository users;

    public RideNotificationService(RideNotificationRepository n, UserAccountRepository u) {
        notifications=n; users=u;
    }

    public void notifyUser(String recipientId, String title, String message, String link, String rideId, String type) {
        if (recipientId == null || recipientId.isBlank()) return;
        RideNotification n = new RideNotification();
        n.setRecipientId(recipientId); n.setTitle(title); n.setMessage(message);
        n.setLink(link); n.setRideId(rideId); n.setType(type);
        notifications.save(n);
    }

    public void notifyAdmins(String title, String message, String link, String rideId, String type) {
        List<UserAccount> admins=users.findByRoleIn(List.of("ADMIN"));
        for (UserAccount admin : admins) notifyUser(admin.getId(),title,message,link,rideId,type);
    }
}
