package fi.poltsi.vempain.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_ACCEPTABLE, reason = "The object already exists/is invalid")
public class DataObjectAlreadyExistsException extends Exception {

}
