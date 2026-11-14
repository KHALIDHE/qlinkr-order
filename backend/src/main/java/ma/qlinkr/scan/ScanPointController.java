package ma.qlinkr.scan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.SecureRandom;
import java.util.List;
import ma.qlinkr.auth.CurrentUser;
import ma.qlinkr.common.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Owner manages scan points and downloads their QR codes. Business always from the token. */
@RestController
@RequestMapping("/api/scan-points")
@PreAuthorize("hasRole('OWNER')")
public class ScanPointController {

    private static final String ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789"; // no 0/o, 1/l/i: easy to read aloud
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ScanPointRepository points;
    private final QrCodeService qr;

    public ScanPointController(ScanPointRepository points, QrCodeService qr) {
        this.points = points;
        this.qr = qr;
    }

    @GetMapping
    public List<ScanPointView> list(@AuthenticationPrincipal Jwt jwt) {
        long bid = CurrentUser.from(jwt).requireBusinessId();
        return points.findByBusinessIdOrderByIdAsc(bid).stream().map(this::view).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ScanPointView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateScanPoint req) {
        long bid = CurrentUser.from(jwt).requireBusinessId();
        String code;
        do {
            code = randomCode();
        } while (points.existsByCode(code));
        return view(points.save(new ScanPoint(bid, code, req.label(), req.kind())));
    }

    @PatchMapping("/{id}")
    @Transactional
    public ScanPointView update(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                @Valid @RequestBody UpdateScanPoint req) {
        ScanPoint p = owned(jwt, id);
        if (req.label() != null) p.rename(req.label());
        if (req.active() != null) p.setActive(req.active());
        return view(p);
    }

    @GetMapping(value = "/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> png(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                      @RequestParam(defaultValue = "600") int size) {
        ScanPoint p = owned(jwt, id);
        int px = Math.max(150, Math.min(size, 2000));
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=qr-" + p.getCode() + ".png")
            .body(qr.png(qr.urlFor(p), px));
    }

    @GetMapping(value = "/{id}/qr.svg", produces = "image/svg+xml")
    public ResponseEntity<String> svg(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        ScanPoint p = owned(jwt, id);
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=qr-" + p.getCode() + ".svg")
            .body(qr.svg(qr.urlFor(p)));
    }

    /** All active scan points on one printable HTML page, 3 per row with their labels. */
    @GetMapping(value = "/print-sheet", produces = MediaType.TEXT_HTML_VALUE)
    public String printSheet(@AuthenticationPrincipal Jwt jwt) {
        long bid = CurrentUser.from(jwt).requireBusinessId();
        StringBuilder html = new StringBuilder("""
            <!doctype html><html><head><meta charset="utf-8"><title>QR codes</title>
            <style>body{font-family:sans-serif}.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:24px}
            .c{text-align:center;page-break-inside:avoid}.c svg{width:180px;height:180px}</style></head>
            <body><div class="grid">""");
        for (ScanPoint p : points.findByBusinessIdOrderByIdAsc(bid)) {
            if (p.isActive()) {
                html.append("<div class=\"c\">").append(qr.svg(qr.urlFor(p)))
                    .append("<div>").append(escape(p.getLabel())).append("</div></div>");
            }
        }
        return html.append("</div></body></html>").toString();
    }

    private ScanPoint owned(Jwt jwt, long id) {
        long bid = CurrentUser.from(jwt).requireBusinessId();
        return points.findByIdAndBusinessId(id, bid).orElseThrow(() -> new NotFoundException("Scan point not found"));
    }

    private ScanPointView view(ScanPoint p) {
        return new ScanPointView(p.getId(), p.getCode(), p.getLabel(), p.getKind(), p.isActive(), qr.urlFor(p));
    }

    static String randomCode() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public record CreateScanPoint(@NotBlank @Size(max = 60) String label, @NotNull ScanPoint.Kind kind) {}

    public record UpdateScanPoint(@Size(min = 1, max = 60) String label, Boolean active) {}

    public record ScanPointView(long id, String code, String label, ScanPoint.Kind kind, boolean active, String url) {}
}
