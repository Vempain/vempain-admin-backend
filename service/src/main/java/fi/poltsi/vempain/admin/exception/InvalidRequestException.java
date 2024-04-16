package fi.poltsi.vempain.admin.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@AllArgsConstructor
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Error processing request")
public class InvalidRequestException extends Exception {
	private final String message;
}
