package fi.poltsi.vempain.admin.exception;

import java.net.URI;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpMethod.GET;

/**
 * Every handler answers a problem detail with a generic message (no exception text leaks) and a correlation id for the log.
 */
class GlobalExceptionHandlerUTC {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static void assertProblem(ProblemDetail problem, HttpStatus status, String detail) {
        assertEquals(status.value(), problem.getStatus());
        assertEquals(detail, problem.getDetail());
        assertEquals(URI.create("about:blank"), problem.getType());
        assertNotNull(problem.getProperties());
        assertNotNull(problem.getProperties()
            .get("correlation_id"));
    }

    @Test
    void notFoundHandlers() {
        assertProblem(handler.handleNoResourceFound(new NoResourceFoundException(GET, "/missing", "/missing")), HttpStatus.NOT_FOUND,
                "The requested resource was not found");
        assertProblem(handler.handleEntityNotFound(new EntityNotFoundException("secret entity")), HttpStatus.NOT_FOUND, "The requested entity was not found");
    }

    @Test
    void responseStatusKeepsTheStatusButNotTheReason() {
        var problem = handler.handleResponseStatus(new ResponseStatusException(HttpStatus.CONFLICT, "secret reason"));

        assertProblem(problem, HttpStatus.CONFLICT, "The request could not be completed");
    }

    @Test
    void badRequestHandlers() {
        assertProblem(handler.handleInvalidRequest(new IllegalArgumentException("secret")), HttpStatus.BAD_REQUEST, "The request was invalid");
        assertProblem(handler.handleInvalidRequest(new InvalidRequestException("Missing component list")), HttpStatus.BAD_REQUEST, "Missing component list");
    }

    @Test
    void authorizationHandlers() {
        assertProblem(handler.handleUnauthorized(new UnauthorizedAccessException()), HttpStatus.UNAUTHORIZED, "Authentication is required");
        assertProblem(handler.handleAccessDenied(new AccessDeniedException("denied")), HttpStatus.FORBIDDEN, "Access to the resource was denied");
    }

    @Test
    void conflictHandlers() {
        assertProblem(handler.handleEntityAlreadyExists(new EntityAlreadyExistsException("Form exists", "form")), HttpStatus.UNPROCESSABLE_ENTITY,
                "Form exists");
        assertProblem(handler.handleDataObjectAlreadyExists(new DataObjectAlreadyExistsException()), HttpStatus.NOT_ACCEPTABLE,
                "The object already exists or is invalid");
    }

    @Test
    void unexpectedExceptionsAreInternalErrorsUnlessAnAccessDenialIsTheCause() {
        assertProblem(handler.handleUnexpected(new IllegalStateException("boom")), HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be completed");
        assertProblem(handler.handleUnexpected(new RuntimeException("wrapped", new IllegalStateException(new AccessDeniedException("deep")))),
                HttpStatus.FORBIDDEN, "Access to the resource was denied");
    }

    @Test
    void everyProblemGetsItsOwnCorrelationId() {
        var first = handler.handleUnexpected(new IllegalStateException("one"));
        var second = handler.handleUnexpected(new IllegalStateException("two"));

        assertNotEquals(first.getProperties()
            .get("correlation_id"),
                second.getProperties()
                    .get("correlation_id"));
    }
}
