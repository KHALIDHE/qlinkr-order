package ma.qlinkr.scan;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanPointRepository extends JpaRepository<ScanPoint, Long> {

    List<ScanPoint> findByBusinessIdOrderByIdAsc(long businessId);

    Optional<ScanPoint> findByIdAndBusinessId(long id, long businessId);

    Optional<ScanPoint> findByCodeAndActiveTrue(String code);

    boolean existsByCode(String code);
}
