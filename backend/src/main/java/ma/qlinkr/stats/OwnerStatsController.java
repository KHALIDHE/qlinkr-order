package ma.qlinkr.stats;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import ma.qlinkr.auth.CurrentUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Owner dashboard numbers, all computed in Oracle SQL. Dates are inclusive, in UTC. */
@RestController
@RequestMapping("/api/owner/stats")
@PreAuthorize("hasRole('OWNER')")
public class OwnerStatsController {

    private final StatsRepository stats;

    public OwnerStatsController(StatsRepository stats) {
        this.stats = stats;
    }

    @GetMapping("/sales-per-day")
    public List<Map<String, Object>> salesPerDay(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return stats.salesPerDay(bid(jwt), from, to);
    }

    @GetMapping("/best-items")
    public List<Map<String, Object>> bestItems(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                               @RequestParam(defaultValue = "10") int limit) {
        return stats.bestItems(bid(jwt), from, to, Math.max(1, Math.min(limit, 50)));
    }

    @GetMapping("/busy-hours")
    public List<Map<String, Object>> busyHours(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return stats.busyHours(bid(jwt), from, to);
    }

    @GetMapping("/preparation-time")
    public Map<String, Object> preparationTime(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return stats.preparationTime(bid(jwt), from, to);
    }

    private static long bid(Jwt jwt) {
        return CurrentUser.from(jwt).requireBusinessId();
    }
}
