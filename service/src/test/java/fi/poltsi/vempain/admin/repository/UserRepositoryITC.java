package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.response.PrivacyType;
import fi.poltsi.vempain.admin.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class UserRepositoryITC extends AbstractITCTest {
	@Test
	@DisplayName("Make sure all injections are in place")
	void injectedComponentsAreNotNull() {
		assertNotNull(userRepository);
	}

	@Test
	@Transactional
	@DisplayName("Create and lock a user")
	void createAndLockUserOk() throws InterruptedException {
		var userId = testITCTools.generateUser();
		var optionalUser = userRepository.findById(userId);
		assertTrue(optionalUser.isPresent());
		var user = optionalUser.get();

		assertTrue(user.getAclId() > 0, "User ACL ID should have been > 0, now it is " + user.getAclId());
		assertEquals(userId, user.getCreator());
		assertNotNull(user.getModifier());
		assertNotNull(user.getModified());
		Assertions.assertFalse(user.isPubliclyVisible());
		assertEquals(PrivacyType.PRIVATE, user.getPrivacyType());
		assertNotNull(user.getBirthday());
		assertNotNull(user.getCreated());
		assertNotNull(user.getDescription());
		assertNotNull(user.getLoginName());
		assertNotNull(user.getModified());
		assertFalse(user.getPob().isEmpty(), "User POB length should have been > 0, now it is empty");

		// Lock user
		String loginName = user.getLoginName();
		user.setLocked(true);
		userRepository.save(user);
		var optionalLockedUser = userRepository.findByLoginName(loginName);
		assertTrue(optionalLockedUser.isPresent(), "The user should have been found by login name");
		var lockedUser = optionalLockedUser.get();
		// TimeUnit.MINUTES.sleep(10);
		assertTrue(lockedUser.isLocked(), "The user should have been locked");
	}

	@Test
	@DisplayName("Fail to create a user without a nick")
	void failUserCreation() {
		var password = testUserAccountTools.encryptPassword(testUserAccountTools.randomLongString());
		var user = User.builder()
					   .aclId(1L)
					   .birthday(Instant.now().minus(20 * 365, ChronoUnit.DAYS))
					   .created(Instant.now().minus(1, ChronoUnit.HOURS))
					   .creator(1L)
					   .description("ITC generated user " + password)
					   .email("first." + password + "@test.tld")
					   .id(1L)
					   .locked(false)
					   .loginName(password)
					   .modified(Instant.now())
					   .modifier(1L)
					   .name("Firstname " + password)
					   .password(testUserAccountTools.encryptPassword(password))
					   .pob("1111")
					   .privacyType(PrivacyType.PRIVATE)
					   .publiclyVisible(false)
					   .street("")
					   .units(null)
					   .build();

		DataIntegrityViolationException dive =
				assertThrows(DataIntegrityViolationException.class,
							 () -> userRepository.save(user),
							 "Expected SQLIntegrityConstraintViolationException because of incomplete user information");
		assertNotNull(dive);
		assertNotNull(dive.getMessage());
		log.info("dive.getMessage: {}", dive.getMessage());
		assertTrue(dive.getMessage().contains("Column 'nick' cannot be null"));
	}
}
