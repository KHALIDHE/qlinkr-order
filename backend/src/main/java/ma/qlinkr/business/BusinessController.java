package ma.qlinkr.business;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import ma.qlinkr.auth.CurrentUser;
import ma.qlinkr.user.AppUser;
import ma.qlinkr.user.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * "My business" endpoints. There is no business id in any URL: the business
 * is always the one in the caller's token.
 */
@RestController
@RequestMapping("/api/business")
public class BusinessController {

    private final BusinessService service;

    public BusinessController(BusinessService service) {
        this.service = service;
    }

    @GetMapping
    public BusinessResponse get(@AuthenticationPrincipal Jwt jwt) {
        long businessId = CurrentUser.from(jwt).requireBusinessId();
        return BusinessResponse.of(service.get(businessId));
    }

    @PatchMapping
    @PreAuthorize("hasRole('OWNER')")
    public BusinessResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateBusinessRequest req) {
        long businessId = CurrentUser.from(jwt).requireBusinessId();
        return BusinessResponse.of(service.update(businessId, req.name(), req.defaultLang(), req.theme()));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('OWNER')")
    public List<MemberResponse> staff(@AuthenticationPrincipal Jwt jwt) {
        long businessId = CurrentUser.from(jwt).requireBusinessId();
        return service.listMembers(businessId).stream().map(MemberResponse::of).toList();
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addStaff(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddStaffRequest req) {
        long businessId = CurrentUser.from(jwt).requireBusinessId();
        return MemberResponse.of(service.addStaff(businessId, req.email(), req.password()));
    }

    @DeleteMapping("/staff/{userId}")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStaff(@AuthenticationPrincipal Jwt jwt, @PathVariable long userId) {
        long businessId = CurrentUser.from(jwt).requireBusinessId();
        service.removeStaff(businessId, userId);
    }

    public record UpdateBusinessRequest(
        @Size(min = 1, max = 120) String name,
        @Pattern(regexp = "ar|fr|en", message = "must be ar, fr or en") String defaultLang,
        @Pattern(regexp = "simple|restaurant|cafe|tearoom|gym|beauty", message = "unknown theme") String theme) {}

    public record AddStaffRequest(
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 10, max = 200, message = "must be at least 10 characters") String password) {}

    public record BusinessResponse(long id, String name, String slug, BusinessType type,
                                   String defaultLang, String theme, Instant createdAt) {
        static BusinessResponse of(Business b) {
            return new BusinessResponse(b.getId(), b.getName(), b.getSlug(), b.getType(),
                b.getDefaultLang(), b.getTheme(), b.getCreatedAt());
        }
    }

    public record MemberResponse(long id, String email, Role role, boolean active) {
        static MemberResponse of(AppUser u) {
            return new MemberResponse(u.getId(), u.getEmail(), u.getRole(), u.isActive());
        }
    }
}
