package ma.qlinkr.common;

/** The request is valid but clashes with existing data, for example an email already in use. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
