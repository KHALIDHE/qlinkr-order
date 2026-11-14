package ma.qlinkr.common;

/**
 * Thrown when a resource does not exist for the caller. A resource that
 * belongs to another business is reported the same way, so the answer never
 * reveals that it exists.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
