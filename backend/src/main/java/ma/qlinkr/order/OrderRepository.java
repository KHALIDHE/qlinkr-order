package ma.qlinkr.order;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByBusinessIdAndIdempotencyKey(long businessId, String idempotencyKey);

    Optional<CustomerOrder> findByPublicToken(String publicToken);

    Optional<CustomerOrder> findByIdAndBusinessId(long id, long businessId);

    List<CustomerOrder> findTop200ByBusinessIdAndStatusInOrderByCreatedAtAsc(long businessId, Collection<OrderStatus> statuses);
}
