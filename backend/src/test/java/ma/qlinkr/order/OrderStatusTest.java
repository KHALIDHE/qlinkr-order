package ma.qlinkr.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Every pair of statuses, allowed or not: 49 checks. */
class OrderStatusTest {

    private static final java.util.Map<OrderStatus, Set<OrderStatus>> ALLOWED = java.util.Map.of(
        OrderStatus.NEW, EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.REJECTED, OrderStatus.CANCELLED),
        OrderStatus.ACCEPTED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
        OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY, OrderStatus.CANCELLED),
        OrderStatus.READY, EnumSet.of(OrderStatus.COMPLETED),
        OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class),
        OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class),
        OrderStatus.REJECTED, EnumSet.noneOf(OrderStatus.class));

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void transitionsFromEachStatus(OrderStatus from) {
        for (OrderStatus to : OrderStatus.values()) {
            assertThat(from.canMoveTo(to)).as(from + " -> " + to).isEqualTo(ALLOWED.get(from).contains(to));
        }
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"COMPLETED", "CANCELLED", "REJECTED"})
    void finalStatusesGoNowhere(OrderStatus status) {
        assertThat(status.isFinal()).isTrue();
    }
}
