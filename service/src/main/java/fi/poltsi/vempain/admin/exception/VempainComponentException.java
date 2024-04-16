package fi.poltsi.vempain.admin.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VempainComponentException extends Exception {
	private final String message;
}
