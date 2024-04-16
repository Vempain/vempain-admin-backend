package fi.poltsi.vempain.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

class AuthToolsUTC {

	@Test
	void passwordCheckOk() {
		if (!AuthTools.passwordCheck("123qweASD^^")) {
			fail("Password check should have succeeded");
		}
	}

	@Test
	void passwordCheckLowercaseOnlyFail() {
		if (AuthTools.passwordCheck("test")) {
			fail("Password check should have failed due to too simple password");
		}
	}

	@Test
	void passwordHash() {
		var password = "123qweASD^^";
		var hashedPassword = AuthTools.passwordHash(password);
		assertNotEquals(password, hashedPassword);
	}
}
