package in.asmj.ride.repo;
import in.asmj.ride.model.RentalVoucher;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List; import java.util.Optional;
public interface RentalVoucherRepository extends MongoRepository<RentalVoucher,String> {
 Optional<RentalVoucher> findByCodeIgnoreCase(String code);
 List<RentalVoucher> findByProviderIdOrderByCreatedAtDesc(String providerId);
 List<RentalVoucher> findByStatusAndExpiresAtAfter(String status, java.time.Instant now);
}
