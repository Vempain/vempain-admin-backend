package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.exception.VempainLayoutException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
class LayoutServiceITC extends AbstractITCTest {
	private final long initCount = 10;

	@AfterEach
	void tearDown() throws ProcessingFailedException, VempainAclException, VempainEntityNotFoundException {
		testITCTools.deleteLayouts();
		testITCTools.deleteAcls();
		testITCTools.checkDatabase();
	}

	@Test
	void getAllLayouts() {
		testITCTools.generateLayouts(initCount);
		Iterable<Layout> layouts = layoutService.findAll();
		Assertions.assertNotNull(layouts);

		for (Layout layout : layouts) {
			assertLayout(layout);
			assertTrue(testITCTools.getLayoutIdList().contains(layout.getId()));
		}
	}

	@Test
	void findById() throws VempainEntityNotFoundException {
		testITCTools.generateLayouts(initCount);
		Layout layout = layoutService.findById(testITCTools.getLayoutIdList().getFirst());
		assertNotNull(layout);
		assertLayout(layout);
	}

	@Test
	void findByNonExistingLayoutId() {
		testITCTools.generateLayouts(initCount);
		try {
			long   nonExistId = Collections.max(testITCTools.getLayoutIdList()) + 1;
			Layout layout     = layoutService.findById(nonExistId);
			fail("A layout should have not been found with id: " + nonExistId + " -> " + layout);
		} catch (VempainEntityNotFoundException e) {
			assertEquals("layout", e.getEntityName());
			assertEquals(VempainMessages.NO_LAYOUT_FOUND_BY_ID, e.getMessage());
		}
	}

	@Test
	void findByLayoutName() throws VempainLayoutException {
		var userId = testITCTools.generateUser();
		var aclId  = testITCTools.generateAcl(userId, null, true, true, true, true);
		var layout = Layout.builder()
						   .layoutName("Test layout 1")
						   .structure("<!--comp_0--><!--comp_1--><!--page--><!--comp_2-->")
						   .locked(false)
						   .aclId(aclId)
						   .creator(userId)
						   .created(Instant.now().minus(1, ChronoUnit.HOURS))
						   .modifier(null)
						   .modified(null)
						   .build();
		layoutRepository.save(layout);
		Layout result = layoutService.findByLayoutName("Test layout 1");
		assertLayout(result);
	}

	@Test
	void findByNonExistingLayoutName() {
		String nonExistName = "ThisDoesNotExist";

		try {
			Layout layout = layoutService.findByLayoutName(nonExistName);
			fail("A layout should have not been found with name: " + nonExistName + " -> " + layout);
		} catch (VempainLayoutException e) {
			assertEquals(VempainMessages.OBJECT_NOT_FOUND, e.getMessage());
		}
	}

	@Test
	void delete() {
		testITCTools.generateLayouts(initCount);
		long layoutId = testITCTools.getLayoutIdList().getFirst();

		try {
			layoutService.delete(layoutId);
		} catch (Exception e) {
			fail("Deleting an existing layout should have not produced an exception: " + e.getMessage());
		}

		try {
			Layout layout = layoutService.findById(layoutId);
			fail("A layout should have not been found with id: " + layoutId + " -> " + layout);
		} catch (VempainEntityNotFoundException e) {
			assertEquals("layout", e.getEntityName());
			assertEquals(VempainMessages.NO_LAYOUT_FOUND_BY_ID, e.getMessage());
		}
	}

	@Test
	void deleteNonExisting() {
		long nonExistingId = 1;

		if (!testITCTools.getLayoutIdList().isEmpty()) {
			nonExistingId = Collections.max(testITCTools.getLayoutIdList()) + 1;
		}

		try {
			layoutService.delete(nonExistingId);
			fail("Deleting non-existing layout ID " + nonExistingId + " should have caused an exception");
		} catch (VempainAclException | ProcessingFailedException e) {
			fail("We should have received an VempainEntityNotFoundException");
		} catch (VempainEntityNotFoundException e) {
			try {
				Layout layout = layoutService.findById(nonExistingId);
				fail("A layout should have not been found with id: " + nonExistingId + " -> " + layout);
			} catch (VempainEntityNotFoundException ex) {
				assertEquals("layout", ex.getEntityName());
				assertEquals(VempainMessages.NO_LAYOUT_FOUND_BY_ID, ex.getMessage());
			}
		}
	}

	@Test
	void save() {
		var userId = testITCTools.generateUser();
		var aclId = testITCTools.generateAcl(userId, null, true, true, true, true);
		Layout layout = Layout.builder()
							  .layoutName("Test layout save")
							  .structure("Test layout structure save")
							  .locked(false)
							  .aclId(aclId)
							  .creator(userId)
							  .created(Instant.now().minus(1, ChronoUnit.HOURS))
							  .modifier(userId)
							  .modified(Instant.now())
							  .build();
		try {
			layoutService.save(layout);
		} catch (Exception e) {
			fail("Creating a layout should have succeeded");
		}
	}

	@Test
	void saveWithoutModifyOk() {
		var userId = testITCTools.generateUser();
		long aclId = testITCTools.generateAcl(userId, null, true, true, true, true);
		Layout layout = Layout.builder()
							  .layoutName("Test layout save")
							  .structure("Test layout structure save")
							  .locked(false)
							  .aclId(aclId)
							  .creator(userId)
							  .created(Instant.now().minus(1, ChronoUnit.HOURS))
							  .build();
		try {
			layoutService.save(layout);
		} catch (Exception e) {
			fail("Creating a layout without modifier information should have succeeded");
		}
	}

	@Test
	void saveFailWithNoName() {
		var userId = testITCTools.generateUser();
		long aclId = testITCTools.generateAcl(userId, null, true, true, true, true);
		Layout layout = Layout.builder()
							  .structure("Test layout structure save")
							  .locked(false)
							  .aclId(aclId)
							  .creator(userId)
							  .created(Instant.now().minus(1, ChronoUnit.HOURS))
							  .modifier(userId)
							  .modified(Instant.now())
							  .build();

		try {
			layoutService.save(layout);
			fail("Creating layout with no name should have failed");
		} catch (VempainAbstractException e) {
			fail("We should have received a VempainAbstractException");
		} catch (VempainLayoutException e) {
			assertEquals("Layout name is null or blank", e.getMessage());
		}
	}

	@Test
	void saveFailWithNoAcl() {
		var userId = testITCTools.generateUser();
		Layout layout = Layout.builder()
							  .layoutName("Test layout save")
							  .structure("Test layout structure save")
							  .locked(false)
							  .creator(userId)
							  .created(Instant.now().minus(1, ChronoUnit.HOURS))
							  .modifier(userId)
							  .modified(Instant.now())
							  .build();

		try {
			layoutService.save(layout);
			fail("Creating layout with no Acl should have failed");
		} catch (VempainLayoutException e) {
			fail("We should have received a VempainAbstractException");
		} catch (VempainAbstractException e) {
			assertTrue(e.getMessage().contains("ACL ID is invalid"));
		}
	}

	@Test
	void saveFailWithNoCreator() throws VempainEntityNotFoundException {
		var userId = testITCTools.generateUser();
		long aclId = testITCTools.generateAcl(userId, null, true, true, true, true);
		Layout layout = Layout.builder()
							  .layoutName("Test layout save")
							  .structure("Test layout structure save")
							  .locked(false)
							  .aclId(aclId)
							  .created(Instant.now().minus(1, ChronoUnit.HOURS))
							  .modifier(userId)
							  .modified(Instant.now())
							  .build();

		try {
			layoutService.save(layout);
			fail("Creating layout with no creator should have failed");
		} catch (VempainLayoutException e) {
			fail("We should have received a VempainAbstractException");
		} catch (VempainAbstractException e) {
			log.info("Exception message:", e);
			assertTrue(e.getMessage().contains("Creator is missing"));
		} finally {
			testITCTools.deleteAcl(aclId);
		}
	}

	@Test
	void saveFailWithNoCreated() {
		var userId = testITCTools.generateUser();
		long aclId = testITCTools.generateAcl(userId, null, true, true, true, true);
		Layout layout = Layout.builder()
							  .layoutName("Test layout save")
							  .structure("Test layout structure save")
							  .locked(false)
							  .aclId(aclId)
							  .creator(userId)
							  .modifier(userId)
							  .modified(Instant.now())
							  .build();

		try {
			layoutService.save(layout);
			fail("Creating layout with no created timestamp should have failed");
		} catch (VempainLayoutException e) {
			fail("We should have received a VempainAbstractException");
		} catch (VempainAbstractException e) {
			assertTrue(e.getMessage().contains("Created datetime is missing"));
		}
	}

	@Test
	void saveFailWithNoModifier() {
		var userId = testITCTools.generateUser();
		long aclId = testITCTools.generateAcl(userId, null, true, true, true, true);
		testITCTools.getAclIdList().add(aclId);
		Layout layout = Layout.builder()
							  .layoutName("Test layout save")
							  .structure("Test layout structure save")
							  .locked(false)
							  .aclId(aclId)
							  .creator(userId)
							  .created(Instant.now().minus(1, ChronoUnit.HOURS))
							  .modified(Instant.now())
							  .build();

		try {
			layoutService.save(layout);
			testITCTools.getLayoutIdList().add(layout.getId());
			fail("Creating layout with no modifier should have failed");
		} catch (VempainLayoutException e) {
			fail("We should have received a VempainAbstractException");
		} catch (VempainAbstractException e) {
			assertTrue(e.getMessage().contains("Modifier is missing"));
		}
	}

	@Test
	void saveFailWithNoModified() throws VempainEntityNotFoundException {
		var userId = testITCTools.generateUser();
		var aclId = testITCTools.generateAcl(userId, null, true, true, true, true);
		Layout layout = Layout.builder()
							  .layoutName("Test layout save")
							  .structure("Test layout structure save")
							  .locked(false)
							  .aclId(aclId)
							  .creator(userId)
							  .created(Instant.now().minus(1, ChronoUnit.HOURS))
							  .modifier(userId)
							  .build();

		try {
			layoutService.save(layout);
			fail("Creating layout with no modified should have failed");
		} catch (VempainLayoutException e) {
			fail("We should have received a VempainAbstractException");
		} catch (VempainAbstractException e) {
			assertTrue(e.getMessage().contains("Modified datetime is missing"));
		} finally {
			testITCTools.deleteAcl(aclId);
		}
	}

	@Test
	void saveFailWithCreatedLaterThanModified() throws VempainEntityNotFoundException {
		var userId = testITCTools.generateUser();
		long aclId = testITCTools.generateAcl(userId, null, true, true, true, true);
		Layout layout = Layout.builder()
							  .layoutName("Test layout save")
							  .structure("Test layout structure save")
							  .locked(false)
							  .aclId(aclId)
							  .creator(1L)
							  .created(Instant.now().plus(3, ChronoUnit.HOURS))
							  .modifier(1L)
							  .modified(Instant.now())
							  .build();

		try {
			layoutService.save(layout);
			fail("Creating layout with no created later than modified should have failed");
		} catch (VempainLayoutException e) {
			fail("We should have received a VempainAbstractException");
		} catch (VempainAbstractException e) {
			assertTrue(e.getMessage().contains("Created datetime is more recent than modified"));
		} finally {
			testITCTools.deleteAcl(aclId);
		}
	}

	// TODO Save fail with no layout name and other missing data

	private void assertLayout(Layout layout) {
		assertNotNull(layout);
		assertNotNull(layout.getCreator());
		assertNotNull(layout.getCreated());

		if (layout.getModifier() != null) {
			assertTrue(layout.getModifier() > 1);
			assertNotNull(layout.getModified());
			assertTrue(layout.getModified().isAfter(layout.getCreated()));
		}

		assertTrue(layout.getId() > 0);
		assertTrue(layout.getAclId() > 0);
		assertNotNull(layout.getLayoutName());
		assertTrue(layout.getLayoutName().contains("Test layout "));
		assertEquals("<!--comp_0--><!--comp_1--><!--page--><!--comp_2-->", layout.getStructure());
	}
}
