package fi.poltsi.vempain.admin.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY, reason = "The object already exists")
public class EntityAlreadyExistsException extends Exception {
	private final String message;
	private final String entityName;

	public EntityAlreadyExistsException(String message, String entityName) {
		super();
		this.message = message;
		this.entityName = entityName;
	}

	public EntityAlreadyExistsException() {
		super();
		this.message = "The object already exists in the repository";
		this.entityName = "";
	}
}
