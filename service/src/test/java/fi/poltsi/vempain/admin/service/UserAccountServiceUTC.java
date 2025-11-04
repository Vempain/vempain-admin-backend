package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UserAccountRepository;
import fi.poltsi.vempain.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static fi.poltsi.vempain.admin.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class UserAccountServiceUTC {
	@Mock
	private UserAccountRepository userAccountRepository;
	@Mock
	private AclRepository         aclRepository;
	private UserService           userService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		userService = new UserService(userAccountRepository, aclRepository);
	}

	@Test
	void findAllOk() {
		List<UserAccount> users = TestUTCTools.generateUserList(10L);
		when(userAccountRepository.findAll()).thenReturn(users);

		try {
			Iterable<UserAccount> returnValue = userService.findAll();
			assertNotNull(returnValue);
			assertEquals(10, StreamSupport.stream(returnValue.spliterator(), false)
										  .count());
		} catch (Exception e) {
			fail("We should not have received any exception");
		}
	}

	@Test
	void findByIdOk() {
		UserAccount userAccount = TestUTCTools.generateUser(ADMIN_ID);
		when(userAccountRepository.findById(ADMIN_ID)).thenReturn(Optional.of(userAccount));

		try {
			Optional<UserAccount> returnUser = userService.findById(1L);
			assertTrue(returnUser.isPresent());
			assertEquals(userAccount, returnUser.get());
		} catch (Exception e) {
			fail("We should not have received any exception");
		}
	}

	@Test
	void lockByIdOk() {
		doNothing().when(userAccountRepository)
				   .lockByUserId(1L);

		try {
			userService.lockUser(1L);
		} catch (Exception e) {
			fail("We should not have received any exception");
		}
	}
}
