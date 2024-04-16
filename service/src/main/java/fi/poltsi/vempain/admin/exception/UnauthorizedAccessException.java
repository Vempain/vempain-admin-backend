package fi.poltsi.vempain.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "Error processing request")
public class UnauthorizedAccessException extends Exception {
}
