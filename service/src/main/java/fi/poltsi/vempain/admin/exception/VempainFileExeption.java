package fi.poltsi.vempain.admin.exception;

public class VempainFileExeption extends Throwable {
	private final String message;
	public VempainFileExeption(String exceptionMessage) {
		this.message = exceptionMessage;
	}
}
