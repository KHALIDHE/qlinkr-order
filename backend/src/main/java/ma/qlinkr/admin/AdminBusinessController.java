package ma.qlinkr.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import ma.qlinkr.business.BusinessController.BusinessResponse;
import ma.qlinkr.business.BusinessService;
import ma.qlinkr.business.BusinessType;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Platform administration: onboarding a new business with its owner account. */
@RestController
@RequestMapping("/api/admin/businesses")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminBusinessController {

    private final BusinessService service;

    public AdminBusinessController(BusinessService service) {
        this.service = service;
    }

    @GetMapping
    public List<BusinessResponse> list() {
        return service.listAll().stream().map(b -> new BusinessResponse(
            b.getId(), b.getName(), b.getSlug(), b.getType(), b.getDefaultLang(), b.getTheme(), b.getCreatedAt()))
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessResponse create(@Valid @RequestBody CreateBusinessRequest req) {
        var b = service.createWithOwner(req.name(), req.slug(), req.type(), req.defaultLang(),
            req.ownerEmail(), req.ownerPassword());
        return new BusinessResponse(b.getId(), b.getName(), b.getSlug(), b.getType(),
            b.getDefaultLang(), b.getTheme(), b.getCreatedAt());
    }

    public record CreateBusinessRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 80)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "lower-case letters, digits and dashes only")
        String slug,
        @NotNull BusinessType type,
        @NotBlank @Pattern(regexp = "ar|fr|en", message = "must be ar, fr or en") String defaultLang,
        @NotBlank @Email @Size(max = 160) String ownerEmail,
        @NotBlank @Size(min = 10, max = 200, message = "must be at least 10 characters") String ownerPassword) {}
}
