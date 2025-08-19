package fi.poltsi.vempain.admin.exception;

import lombok.Getter;

import java.nio.file.Path;

@Getter
public class VempainIngestException extends Exception {
	private final Path storedFile;

	public VempainIngestException(String message, Throwable cause, Path storedFile) {
		super(message, cause);
		this.storedFile = storedFile;
	}
}
