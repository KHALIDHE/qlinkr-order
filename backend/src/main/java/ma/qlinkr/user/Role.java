package ma.qlinkr.user;

public enum Role {
    /** Runs the platform. Belongs to no business. */
    PLATFORM_ADMIN,
    /** Manages one business: catalogue, staff, settings. */
    OWNER,
    /** Works in one business: sees and updates orders. */
    STAFF
}
