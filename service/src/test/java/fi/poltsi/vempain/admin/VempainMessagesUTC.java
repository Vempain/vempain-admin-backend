package fi.poltsi.vempain.admin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VempainMessagesUTC {
	@Test
	void messagesOk() {
		assertEquals("User session is not valid", VempainMessages.INVALID_USER_SESSION);
		assertEquals("User attempted an action without proper session", VempainMessages.INVALID_USER_SESSION_MSG);
		assertEquals("Insufficient user permissions", VempainMessages.UNAUTHORIZED_ACCESS);
		assertEquals("Failed to find layout by ID", VempainMessages.NO_LAYOUT_FOUND_BY_ID);
	}
}
