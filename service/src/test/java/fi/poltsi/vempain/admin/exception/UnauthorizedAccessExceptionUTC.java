package fi.poltsi.vempain.admin.exception;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
class UnauthorizedAccessExceptionUTC {
	@Test
	void throwOk() {
		try {
			throw new UnauthorizedAccessException();
		} catch (UnauthorizedAccessException e) {
			log.info("Exception: ", e);
			assertNull(e.getMessage());
		}
	}
}
