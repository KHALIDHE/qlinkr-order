package ma.qlinkr.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByBusinessIdOrderByPositionAscIdAsc(long businessId);

    Optional<Category> findByIdAndBusinessId(long id, long businessId);
}
