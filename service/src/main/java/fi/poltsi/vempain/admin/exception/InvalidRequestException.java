package fi.poltsi.vempain.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Error processing request")
public class InvalidRequestException extends Exception {

    private final String message;
}
