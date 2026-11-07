package in.asmj.ride.service;

import in.asmj.ride.model.RideBooking;
import in.asmj.ride.model.UserAccount;
import in.asmj.ride.repo.RideBookingRepository;
import in.asmj.ride.repo.VehicleRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/** Checks both the driver's approval/availability flag and overlapping outward/return assignments. */
@Service
public class DriverAvailabilityService {
    private final RideBookingRepository rides;
    private final VehicleRepository vehicles;

    public DriverAvailabilityService(RideBookingRepository rides, VehicleRepository vehicles) {
        this.rides = rides;
        this.vehicles = vehicles;
    }

    public boolean isAvailable(UserAccount driver, LocalDateTime start, LocalDateTime end, String excludeRideId) {
        return driver != null && "DRIVER".equals(driver.getRole())
                && "APPROVED".equals(driver.getStatus()) && driver.isMobileVerified()
                && driver.isAvailable() && !hasConflict(driver, start, end, excludeRideId);
    }

    /** Used when retaining the same outward driver for a return leg, even if their availability toggle changed. */
    public boolean hasConflict(UserAccount driver, LocalDateTime start, LocalDateTime end, String excludeRideId) {
        if (driver == null || start == null || end == null || !end.isAfter(start)) return true;
        Set<String> vehicleIds = new HashSet<>();
        vehicles.findByAssignedDriverId(driver.getId()).forEach(v -> vehicleIds.add(v.getId()));
        if ("DRIVER".equals(driver.getRole())) vehicles.findByOwnerId(driver.getId()).forEach(v -> vehicleIds.add(v.getId()));
        LocalDateTime now = LocalDateTime.now();

        for (RideBooking other : rides.findAll()) {
            if (excludeRideId != null && excludeRideId.equals(other.getId())) continue;
            if ("CANCELLED".equals(other.getStatus())) continue;

            boolean requestedVehicleLinked = other.getAssignedDriverId() == null && other.getRequestedVehicleId() != null
                    && vehicleIds.contains(other.getRequestedVehicleId());
            boolean outwardLinked = driver.getId().equals(other.getAssignedDriverId()) || requestedVehicleLinked;
            boolean returnLinked = driver.getId().equals(other.getReturnAssignedDriverId());
            if (!outwardLinked && !returnLinked) continue;

            LocalDateTime bookingStart = other.getScheduledAt() != null ? other.getScheduledAt()
                    : LocalDateTime.ofInstant(other.getCreatedAt() == null ? Instant.now() : other.getCreatedAt(), ZoneId.systemDefault());

            if (outwardLinked) {
                LocalDateTime busyStart = bookingStart;
                LocalDateTime busyEnd = busyStart.plusHours(4);
                boolean activeOutward = List.of("ACCEPTED", "ASSIGNED", "IN_PROGRESS").contains(other.getStatus());
                if (activeOutward && ("IN_PROGRESS".equals(other.getStatus()) || other.getScheduledAt() == null || !busyEnd.isAfter(now))) {
                    busyStart = now;
                    busyEnd = now.plusHours(4);
                }
                boolean returnStillReserved = "ROUND_TRIP".equals(other.getJourneyType())
                        && other.getReturnAt() != null
                        && !List.of("COMPLETED", "CANCELLED", "NOT_APPLICABLE").contains(other.getReturnLegStatus())
                        && (driver.getId().equals(other.getReturnAssignedDriverId())
                            || (other.getReturnAssignedDriverId() == null && requestedVehicleLinked));
                if (returnStillReserved) {
                    LocalDateTime returnEnd = other.getReturnAt().plusHours(4);
                    if (returnEnd.isBefore(now.plusHours(4))) returnEnd = now.plusHours(4);
                    if (returnEnd.isAfter(busyEnd)) busyEnd = returnEnd;
                }
                if (overlaps(busyStart, busyEnd, start, end)) return true;
            }

            if (returnLinked && "ROUND_TRIP".equals(other.getJourneyType()) && other.getReturnAt() != null
                    && !List.of("COMPLETED", "CANCELLED", "NOT_APPLICABLE").contains(other.getReturnLegStatus())) {
                LocalDateTime busyStart = other.getReturnAt();
                LocalDateTime busyEnd = busyStart.plusHours(4);
                if ("IN_PROGRESS".equals(other.getReturnLegStatus()) || !busyEnd.isAfter(now)) {
                    busyStart = now;
                    busyEnd = now.plusHours(4);
                }
                if (overlaps(busyStart, busyEnd, start, end)) return true;
            }
        }
        return false;
    }

    private boolean overlaps(LocalDateTime busyStart, LocalDateTime busyEnd, LocalDateTime requestedStart, LocalDateTime requestedEnd) {
        return busyStart.isBefore(requestedEnd.plusHours(1)) && busyEnd.isAfter(requestedStart.minusHours(1));
    }
}
