package fi.poltsi.vempain.admin.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NoResourceFoundException.class)
	public ProblemDetail handleNoResourceFound(NoResourceFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "The requested resource was not found", exception);
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public ProblemDetail handleEntityNotFound(EntityNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "The requested entity was not found", exception);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ProblemDetail handleResponseStatus(ResponseStatusException exception) {
		return problem(HttpStatus.valueOf(exception.getStatusCode()
		                                           .value()), "The request could not be completed", exception);
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
	public ProblemDetail handleInvalidRequest(Exception exception) {
		return problem(HttpStatus.BAD_REQUEST, "The request was invalid", exception);
	}

	@ExceptionHandler(InvalidRequestException.class)
	public ProblemDetail handleInvalidRequest(InvalidRequestException exception) {
		return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
	}

	@ExceptionHandler(UnauthorizedAccessException.class)
	public ProblemDetail handleUnauthorized(UnauthorizedAccessException exception) {
		return problem(HttpStatus.UNAUTHORIZED, "Authentication is required", exception);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ProblemDetail handleAccessDenied(AccessDeniedException exception) {
		return problem(HttpStatus.FORBIDDEN, "Access to the resource was denied", exception);
	}

	@ExceptionHandler(EntityAlreadyExistsException.class)
	public ProblemDetail handleEntityAlreadyExists(EntityAlreadyExistsException exception) {
		return problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), exception);
	}

	@ExceptionHandler(DataObjectAlreadyExistsException.class)
	public ProblemDetail handleDataObjectAlreadyExists(DataObjectAlreadyExistsException exception) {
		return problem(HttpStatus.NOT_ACCEPTABLE, "The object already exists or is invalid", exception);
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception exception) {
		if (hasCause(exception, AccessDeniedException.class)) {
			return problem(HttpStatus.FORBIDDEN, "Access to the resource was denied", exception);
		}
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be completed", exception);
	}

	private boolean hasCause(Throwable exception, Class<? extends Throwable> type) {
		for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
			if (type.isInstance(cause)) {
				return true;
			}
		}
		return false;
	}

	private ProblemDetail problem(HttpStatus status, String detail, Exception exception) {
		var correlationId = UUID.randomUUID()
		                        .toString();
		log.warn("Request failed with correlation id {}: {}", correlationId, exception.getMessage(), exception);
		var problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create("about:blank"));
		problem.setProperty("correlation_id", correlationId);
		return problem;
	}
}
