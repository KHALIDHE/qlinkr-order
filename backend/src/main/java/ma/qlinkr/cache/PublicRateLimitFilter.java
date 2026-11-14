package ma.qlinkr.cache;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limits on the public endpoints, counted in Redis so they hold across
 * application instances. Fixed windows: one counter per key per window.
 *
 *   browsing a catalogue   per IP
 *   placing an order       per IP and per scan point
 *
 * The counter is incremented and given its expiry in one Lua script, so it
 * can never be left without an expiry. If Redis is down, requests are let
 * through: a broken limiter must not take the shop offline.
 */
@Component
public class PublicRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PublicRateLimitFilter.class);
    private static final Pattern ORDER_PATH = Pattern.compile("^/api/public/scan/([^/]+)/orders$");

    private static final DefaultRedisScript<Long> INCR_WITH_EXPIRY = new DefaultRedisScript<>("""
        local n = redis.call('INCR', KEYS[1])
        if n == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
        return n""", Long.class);

    private final StringRedisTemplate redis;
    private final Duration window;
    private final int browsePerIp;
    private final int ordersPerIp;
    private final int ordersPerScanPoint;

    public PublicRateLimitFilter(StringRedisTemplate redis,
                                 @Value("${qlinkr.rate-limit.window:PT1M}") Duration window,
                                 @Value("${qlinkr.rate-limit.browse-per-ip:120}") int browsePerIp,
                                 @Value("${qlinkr.rate-limit.orders-per-ip:10}") int ordersPerIp,
                                 @Value("${qlinkr.rate-limit.orders-per-scan-point:30}") int ordersPerScanPoint) {
        this.redis = redis;
        this.window = window;
        this.browsePerIp = browsePerIp;
        this.ordersPerIp = ordersPerIp;
        this.ordersPerScanPoint = ordersPerScanPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/public/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        long slot = System.currentTimeMillis() / window.toMillis();
        Matcher order = ORDER_PATH.matcher(request.getRequestURI());
        boolean allowed;
        if (order.matches() && "POST".equals(request.getMethod())) {
            allowed = under("rl:order:ip:" + ip + ":" + slot, ordersPerIp)
                && under("rl:order:sp:" + order.group(1) + ":" + slot, ordersPerScanPoint);
        } else {
            allowed = under("rl:browse:ip:" + ip + ":" + slot, browsePerIp);
        }
        if (!allowed) {
            long retry = window.toSeconds() - (System.currentTimeMillis() / 1000) % Math.max(1, window.toSeconds());
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(Math.max(1, retry)));
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"title\":\"Too Many Requests\",\"status\":429,"
                + "\"detail\":\"Too many requests, please wait a moment\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean under(String key, int limit) {
        try {
            Long count = redis.execute(INCR_WITH_EXPIRY, List.of(key), String.valueOf(window.toMillis()));
            return count == null || count <= limit;
        } catch (RuntimeException e) {
            log.warn("Rate limiter unavailable, letting the request through: {}", e.getMessage());
            return true;
        }
    }
}
