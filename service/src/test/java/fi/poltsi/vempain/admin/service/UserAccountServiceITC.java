package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import org.junit.jupiter.api.Test;

import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountServiceITC extends AbstractITCTest {
	private static final Long count = 10L;
	@Test
	void findByIdOk() {
		var userId = testITCTools.generateUser();
		var userOptional = userService.findById(userId);
		assertTrue(userOptional.isPresent());
		assertEquals(userId, userOptional.get().getId());
		userService.lockUser(userId);
	}

	@Test
	void findAllOk() {
		var idList = testITCTools.generateUsers(count);
		var users = userService.findAll();
		assertEquals(count, StreamSupport.stream(users.spliterator(), false).count());
	}
}
