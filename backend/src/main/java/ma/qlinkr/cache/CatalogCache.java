package ma.qlinkr.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import ma.qlinkr.catalog.CatalogController.CatalogChanged;
import ma.qlinkr.catalog.Lang;
import ma.qlinkr.publicapi.PublicCatalogService.CategoryView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Caches the public catalogue in Redis, per business and language.
 *
 * The catalogue is read on every scan and changes rarely, so it is a good
 * cache candidate. Every catalogue edit or stock change publishes
 * CatalogChanged, which deletes the three language entries at once: the next
 * scan rebuilds them, so an edit is visible immediately. A TTL is a safety net
 * in case an eviction is ever missed.
 *
 * If Redis is down, the cache is skipped and the database answers.
 */
@Component
public class CatalogCache {

    private static final Logger log = LoggerFactory.getLogger(CatalogCache.class);
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<List<CategoryView>> TYPE = new TypeReference<>() { };

    private final StringRedisTemplate redis;

    public CatalogCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    static String key(long businessId, String lang) {
        return "catalog:" + businessId + ":" + lang;
    }

    public Optional<List<CategoryView>> get(long businessId, String lang) {
        try {
            String json = redis.opsForValue().get(key(businessId, lang));
            return json == null ? Optional.empty() : Optional.of(JSON.readValue(json, TYPE));
        } catch (RuntimeException e) {
            log.warn("Catalogue cache read failed, using the database: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void put(long businessId, String lang, List<CategoryView> catalog) {
        try {
            redis.opsForValue().set(key(businessId, lang), JSON.writeValueAsString(catalog), TTL);
        } catch (RuntimeException e) {
            log.warn("Catalogue cache write failed: {}", e.getMessage());
        }
    }

    @EventListener
    public void onCatalogChanged(CatalogChanged event) {
        try {
            redis.delete(Lang.ALL.stream().map(lang -> key(event.businessId(), lang)).toList());
        } catch (RuntimeException e) {
            log.warn("Catalogue cache eviction failed for business {}: {}", event.businessId(), e.getMessage());
        }
    }
}
