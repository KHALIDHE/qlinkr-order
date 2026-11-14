package ma.qlinkr.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns exceptions into one consistent JSON error format (RFC 9457 problem
 * details): type, title, status, detail, plus "errors" for validation.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException e) {
        return problem(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ma.qlinkr.order.NoLongerAvailableException.class)
    ProblemDetail noLongerAvailable(ma.qlinkr.order.NoLongerAvailableException e) {
        ProblemDetail body = problem(HttpStatus.CONFLICT, "Some items are no longer available");
        body.setProperty("unavailableItemIds", e.itemIds());
        return body;
    }

    @ExceptionHandler(ma.qlinkr.order.InvalidTransitionException.class)
    ProblemDetail invalidTransition(ma.qlinkr.order.InvalidTransitionException e) {
        return problem(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({org.springframework.web.method.annotation.HandlerMethodValidationException.class,
                       org.springframework.web.bind.MissingRequestHeaderException.class})
    ProblemDetail invalidRequestPart(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request: " + e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail badCredentials(BadCredentialsException e) {
        return problem(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accessDenied(AccessDeniedException e) {
        return problem(HttpStatus.FORBIDDEN, "You are not allowed to do this");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
            .forEach(err -> errors.putIfAbsent(err.getField(), err.getDefaultMessage()));
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Some fields are invalid");
        body.setProperty("errors", errors);
        return body;
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(status.getReasonPhrase());
        return body;
    }
}
