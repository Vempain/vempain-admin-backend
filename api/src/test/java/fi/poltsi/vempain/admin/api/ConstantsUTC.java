package fi.poltsi.vempain.admin.api;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstantsUTC {
	@Test
	void pathPrefixesAndHeaderArePinned() {
		assertEquals(1L, Constants.ADMIN_ID);
		assertEquals("/content-management", Constants.REST_CONTENT_PREFIX);
		assertEquals("/content-management/file", Constants.REST_FILE_PREFIX);
		assertEquals("/content-management/data", Constants.REST_DATA_PREFIX);
		assertEquals("/schedule-management", Constants.REST_SCHEDULE_PREFIX);
		assertEquals("/admin-management", Constants.REST_ADMIN_PREFIX);
		assertEquals("/login", Constants.LOGIN_PATH);
		assertEquals("/test", Constants.TEST_PATH_PREFIX);
		assertEquals("X-Vempain-Api-Token", Constants.API_TOKEN_HEADER);
	}

	@Test
	void constantsCannotBeInstantiated() throws Exception {
		Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
		assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);

		var thrown = assertThrows(InvocationTargetException.class, constructor::newInstance);

		assertInstanceOf(IllegalStateException.class, thrown.getCause());
		assertEquals("Constants class", thrown.getCause()
											  .getMessage());
	}
}
