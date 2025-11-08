package fi.poltsi.vempain.admin.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@AllArgsConstructor
@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Error processing request")
public class ProcessingFailedException extends Exception {
	private final String message;
}
