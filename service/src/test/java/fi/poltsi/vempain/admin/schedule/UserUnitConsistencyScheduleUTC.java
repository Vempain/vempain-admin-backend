package fi.poltsi.vempain.admin.schedule;

import fi.poltsi.vempain.admin.entity.Unit;
import fi.poltsi.vempain.admin.entity.User;
import fi.poltsi.vempain.admin.service.UnitService;
import fi.poltsi.vempain.admin.service.UserService;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

class UserUnitConsistencyScheduleUTC {
	@Mock
	UnitService unitService;
	@Mock
	UserService userService;

	private UserUnitConsistencySchedule userUnitConsistencySchedule;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		userUnitConsistencySchedule = new UserUnitConsistencySchedule(unitService, userService);
	}

	@Test
	void verifyOk() {
		List<User> users = TestUTCTools.generateUserList(5L);
		List<Unit> units = TestUTCTools.generateUnitList(5L);

		// We add a user with non-empty unit list
		users.get(0).setUnits(Collections.singleton(units.get(0)));

		when(userService.findAll()).thenReturn(users);
		when(unitService.findAll()).thenReturn(units);

		try {
			userUnitConsistencySchedule.verify();
		} catch (Exception e) {
			fail("Should not have received any exceptions for normal run: " + e);
		}
	}
}
