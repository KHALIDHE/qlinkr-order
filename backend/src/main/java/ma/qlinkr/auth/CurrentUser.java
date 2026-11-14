package ma.qlinkr.auth;

import ma.qlinkr.user.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Who is calling, read from a token whose signature Spring Security already
 * checked. Services take the business id from here, never from the URL or the
 * request body, so a user cannot act on another business by changing an id.
 */
public record CurrentUser(long userId, Long businessId, Role role, String email) {

    static final String CLAIM_EMAIL = "email";
    static final String CLAIM_ROLE = "role";
    static final String CLAIM_BUSINESS = "bid";

    public static CurrentUser from(Jwt jwt) {
        Number bid = jwt.getClaim(CLAIM_BUSINESS);
        return new CurrentUser(
            Long.parseLong(jwt.getSubject()),
            bid == null ? null : bid.longValue(),
            Role.valueOf(jwt.getClaimAsString(CLAIM_ROLE)),
            jwt.getClaimAsString(CLAIM_EMAIL));
    }

    /** The business this user works for. A platform admin has none and is refused. */
    public long requireBusinessId() {
        if (businessId == null) {
            throw new AccessDeniedException("This account does not belong to a business");
        }
        return businessId;
    }
}
