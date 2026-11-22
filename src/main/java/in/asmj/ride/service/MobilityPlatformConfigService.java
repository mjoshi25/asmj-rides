package in.asmj.ride.service;

import in.asmj.ride.model.MobilityPlatformConfig;
import in.asmj.ride.repo.MobilityPlatformConfigRepository;
import org.springframework.stereotype.Service;

@Service
public class MobilityPlatformConfigService {
    private final MobilityPlatformConfigRepository repository;
    public MobilityPlatformConfigService(MobilityPlatformConfigRepository repository) { this.repository = repository; }

    /** Returns defaults without writing to MongoDB; explicit Admin configuration always wins. */
    public MobilityPlatformConfig getCurrent() {
        return repository.findById("default").orElseGet(MobilityPlatformConfig::new);
    }

    public double commissionForCategory(String category) { return getCurrent().commissionFor(category); }

    public boolean isCityRolloutEnabled() {
        return "CITY".equalsIgnoreCase(getCurrent().getServiceCoverageMode());
    }
}
