package ma.qlinkr.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Every lookup is scoped to one business. */
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByBusinessIdOrderByCategoryIdAscPositionAscIdAsc(long businessId);

    Optional<Item> findByIdAndBusinessId(long id, long businessId);
}
