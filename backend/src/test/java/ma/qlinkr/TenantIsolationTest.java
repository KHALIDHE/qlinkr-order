package ma.qlinkr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

/**
 * Two businesses, A and B, each with an owner and a staff member. Every
 * test tries to cross from one to the other, or to go above its role.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantIsolationTest extends IntegrationTest {

    private Tenant a;
    private Tenant b;

    record Tenant(String slug, String ownerEmail, String ownerToken, long ownerId,
                  String staffEmail, String staffToken, long staffId) {}

    @BeforeAll
    void createTwoBusinesses() {
        String admin = adminToken();
        a = createTenant(admin, "cafe-a");
        b = createTenant(admin, "shop-b");
    }

    private Tenant createTenant(String adminToken, String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String slug = prefix + "-" + suffix;
        String ownerEmail = "owner-" + slug + "@qlinkr.test";
        String staffEmail = "staff-" + slug + "@qlinkr.test";
        String password = "password-" + suffix;

        Response created = post("/api/admin/businesses", adminToken, Map.of(
            "name", "Business " + slug, "slug", slug, "type", "RESTAURANT", "defaultLang", "fr",
            "ownerEmail", ownerEmail, "ownerPassword", password));
        assertThat(created.status()).as(created.raw()).isEqualTo(201);

        String ownerToken = login(ownerEmail, password);
        Response staff = post("/api/business/staff", ownerToken, Map.of("email", staffEmail, "password", password));
        assertThat(staff.status()).as(staff.raw()).isEqualTo(201);

        long ownerId = ((Number) get("/api/auth/me", ownerToken).map().get("id")).longValue();
        long staffId = ((Number) staff.map().get("id")).longValue();
        return new Tenant(slug, ownerEmail, ownerToken, ownerId, staffEmail, login(staffEmail, password), staffId);
    }

    @Test
    void eachMemberSeesOnlyTheirOwnBusiness() {
        assertThat(get("/api/business", a.ownerToken()).map()).containsEntry("slug", a.slug());
        assertThat(get("/api/business", a.staffToken()).map()).containsEntry("slug", a.slug());
        assertThat(get("/api/business", b.ownerToken()).map()).containsEntry("slug", b.slug());
    }

    @Test
    void staffListContainsOnlyOwnMembers() {
        List<Map<String, Object>> members = get("/api/business/staff", a.ownerToken()).list();

        assertThat(members).extracting(m -> m.get("email"))
            .containsExactlyInAnyOrder(a.ownerEmail(), a.staffEmail());
    }

    @Test
    void ownerCannotRemoveStaffOfAnotherBusiness() {
        Response res = call(HttpMethod.DELETE, "/api/business/staff/" + b.staffId(), a.ownerToken(), null);

        // 404, not 403: the answer must not confirm that this id exists elsewhere.
        assertThat(res.status()).isEqualTo(404);
        assertThat(get("/api/business/staff", b.ownerToken()).list())
            .extracting(m -> m.get("email")).contains(b.staffEmail());
    }

    @Test
    void ownerCannotRemoveAnOwnerThroughTheStaffEndpoint() {
        assertThat(call(HttpMethod.DELETE, "/api/business/staff/" + a.ownerId(), a.ownerToken(), null).status())
            .isEqualTo(404);
        assertThat(call(HttpMethod.DELETE, "/api/business/staff/" + b.ownerId(), a.ownerToken(), null).status())
            .isEqualTo(404);
    }

    @Test
    void renamingChangesOnlyTheCallersBusiness() {
        Response res = call(HttpMethod.PATCH, "/api/business", a.ownerToken(), Map.of("name", "Renamed A"));

        assertThat(res.status()).isEqualTo(200);
        assertThat(get("/api/business", a.ownerToken()).map()).containsEntry("name", "Renamed A");
        assertThat(get("/api/business", b.ownerToken()).map().get("name")).isNotEqualTo("Renamed A");
    }

    static Stream<Arguments> ownerOnlyEndpoints() {
        return Stream.of(
            Arguments.of(HttpMethod.PATCH, "/api/business", Map.of("name", "Hacked")),
            Arguments.of(HttpMethod.GET, "/api/business/staff", null),
            Arguments.of(HttpMethod.POST, "/api/business/staff",
                Map.of("email", "intruder@qlinkr.test", "password", "password-12345")),
            Arguments.of(HttpMethod.DELETE, "/api/business/staff/1", null));
    }

    @ParameterizedTest(name = "staff gets 403 on {0} {1}")
    @MethodSource("ownerOnlyEndpoints")
    void staffCannotManageTheBusiness(HttpMethod method, String path, Object body) {
        assertThat(call(method, path, a.staffToken(), body).status()).isEqualTo(403);
    }

    static Stream<Arguments> adminEndpoints() {
        return Stream.of(
            Arguments.of(HttpMethod.GET, "/api/admin/businesses", null),
            Arguments.of(HttpMethod.POST, "/api/admin/businesses", Map.of(
                "name", "X", "slug", "x-" + UUID.randomUUID().toString().substring(0, 6), "type", "SHOP",
                "defaultLang", "en", "ownerEmail", "x@qlinkr.test", "ownerPassword", "password-12345")));
    }

    @ParameterizedTest(name = "owner and staff get 403 on {0} {1}")
    @MethodSource("adminEndpoints")
    void membersCannotUseAdminEndpoints(HttpMethod method, String path, Object body) {
        assertThat(call(method, path, a.ownerToken(), body).status()).isEqualTo(403);
        assertThat(call(method, path, a.staffToken(), body).status()).isEqualTo(403);
    }

    @Test
    void platformAdminHasNoBusinessOfItsOwn() {
        assertThat(get("/api/business", adminToken()).status()).isEqualTo(403);
    }

    @Test
    void anEmailCanBelongToOnlyOneAccount() {
        Response res = post("/api/business/staff", b.ownerToken(),
            Map.of("email", a.staffEmail().toUpperCase(), "password", "password-12345"));

        assertThat(res.status()).isEqualTo(409);
    }

    @Test
    void duplicateSlugIsRefused() {
        Response res = post("/api/admin/businesses", adminToken(), Map.of(
            "name", "Copy", "slug", a.slug(), "type", "SHOP", "defaultLang", "en",
            "ownerEmail", "copy-" + UUID.randomUUID() + "@qlinkr.test", "ownerPassword", "password-12345"));

        assertThat(res.status()).isEqualTo(409);
    }
}
