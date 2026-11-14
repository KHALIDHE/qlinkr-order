package ma.qlinkr.stats;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Dashboard queries written as SQL, not Java loops: the database groups,
 * ranks and averages close to the data, and one round trip returns the answer.
 * Cancelled and rejected orders never count as sales.
 */
@Repository
public class StatsRepository {

    static final String SALES_PER_DAY = """
        SELECT day, orders, revenue_cents,
               ROUND(AVG(revenue_cents) OVER (ORDER BY day ROWS BETWEEN 6 PRECEDING AND CURRENT ROW)) AS revenue_7d_avg_cents
        FROM (
            SELECT TRUNC(CAST(o.created_at AT TIME ZONE 'UTC' AS DATE)) AS day,
                   COUNT(*) AS orders,
                   SUM(o.total_cents) AS revenue_cents
            FROM customer_order o
            WHERE o.business_id = ?
              AND o.created_at >= ? AND o.created_at < ?
              AND o.status NOT IN ('CANCELLED', 'REJECTED')
            GROUP BY TRUNC(CAST(o.created_at AT TIME ZONE 'UTC' AS DATE))
        )
        ORDER BY day""";

    static final String BEST_ITEMS = """
        SELECT item_id, item_name, quantity, revenue_cents,
               RANK() OVER (ORDER BY quantity DESC) AS rank_by_quantity
        FROM (
            SELECT l.item_id,
                   MAX(l.item_name) AS item_name,
                   SUM(l.quantity) AS quantity,
                   SUM(l.quantity * l.unit_price_cents) AS revenue_cents
            FROM customer_order o
            JOIN order_line l ON l.order_id = o.id
            WHERE o.business_id = ?
              AND o.created_at >= ? AND o.created_at < ?
              AND o.status NOT IN ('CANCELLED', 'REJECTED')
            GROUP BY l.item_id
        )
        ORDER BY quantity DESC, item_id
        FETCH FIRST ? ROWS ONLY""";

    static final String BUSY_HOURS = """
        SELECT EXTRACT(HOUR FROM (o.created_at AT TIME ZONE 'UTC')) AS hour,
               COUNT(*) AS orders,
               ROUND(100 * RATIO_TO_REPORT(COUNT(*)) OVER (), 1) AS share_percent
        FROM customer_order o
        WHERE o.business_id = ?
          AND o.created_at >= ? AND o.created_at < ?
          AND o.status NOT IN ('CANCELLED', 'REJECTED')
        GROUP BY EXTRACT(HOUR FROM (o.created_at AT TIME ZONE 'UTC'))
        ORDER BY hour""";

    /** Seconds between ACCEPTED and READY, for orders that reached READY. */
    static final String PREPARATION_TIME = """
        SELECT COUNT(*) AS orders,
               ROUND(AVG(secs)) AS avg_seconds,
               ROUND(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY secs)) AS median_seconds,
               ROUND(PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY secs)) AS p90_seconds
        FROM (
            SELECT EXTRACT(DAY FROM (o.ready_at - o.accepted_at)) * 86400
                 + EXTRACT(HOUR FROM (o.ready_at - o.accepted_at)) * 3600
                 + EXTRACT(MINUTE FROM (o.ready_at - o.accepted_at)) * 60
                 + EXTRACT(SECOND FROM (o.ready_at - o.accepted_at)) AS secs
            FROM customer_order o
            WHERE o.business_id = ?
              AND o.created_at >= ? AND o.created_at < ?
              AND o.accepted_at IS NOT NULL AND o.ready_at IS NOT NULL
        )""";

    private final JdbcTemplate jdbc;

    public StatsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> salesPerDay(long businessId, LocalDate from, LocalDate to) {
        return jdbc.queryForList(SALES_PER_DAY, businessId, start(from), endExclusive(to));
    }

    public List<Map<String, Object>> bestItems(long businessId, LocalDate from, LocalDate to, int limit) {
        return jdbc.queryForList(BEST_ITEMS, businessId, start(from), endExclusive(to), limit);
    }

    public List<Map<String, Object>> busyHours(long businessId, LocalDate from, LocalDate to) {
        return jdbc.queryForList(BUSY_HOURS, businessId, start(from), endExclusive(to));
    }

    public Map<String, Object> preparationTime(long businessId, LocalDate from, LocalDate to) {
        return jdbc.queryForMap(PREPARATION_TIME, businessId, start(from), endExclusive(to));
    }

    static OffsetDateTime start(LocalDate day) {
        return day.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    static OffsetDateTime endExclusive(LocalDate day) {
        return day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
    }
}
