package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.entity.Acl;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
class ComponentServiceITC extends AbstractITCTest {
	private final long initCount  = 10L;

	@AfterEach
	void tearDown() throws VempainEntityNotFoundException {
		testITCTools.deleteComponents();
	}

	@Test
	void findAllOk() throws VempainComponentException, VempainAbstractException {
		var comps = componentService.findAll();
		log.info("ZZZZ Found components: {}", comps);
		testITCTools.generateComponents(initCount);
		var components = componentService.findAll();
		log.info("ZZZZ Generated components: {}", components);
		assertEquals(initCount, components.size());

		for (Component component : components) {
			assertComponent(component);
		}
	}

	@Test
	void findByIdOk() throws VempainComponentException, VempainAbstractException {
		testITCTools.generateComponents(initCount);
		Component component = componentService.findById(testITCTools.getComponentIdList().get(0));
		assertComponent(component);
	}

	@Test
	void deleteByIdOk() throws VempainEntityNotFoundException, VempainComponentException, VempainAbstractException {
		testITCTools.generateComponents(initCount);
		Long componentId = testITCTools.getComponentIdList().get(0);
		long aclId       = componentService.findById(componentId).getAclId();

		componentService.deleteById(componentId);

		try {
			Component component1 = componentService.findById(componentId);
			fail("We should not have found a component. Got: " + component1);
		} catch (VempainComponentException e) {
			assertEquals("Failed to find component", e.getMessage());
		}

		Iterable<Acl> acls = aclService.findAclByAclId(aclId);
		assertEquals(0, StreamSupport.stream(acls.spliterator(), false).count());
	}

	@Test
	void saveWithNoAclFail() {
		var userId = testITCTools.generateUser();
		Component component = Component.builder()
									   .compName("Test component fail")
									   .compData("<!-- component data fail -->")
									   .creator(userId)
									   .created(Instant.now().minus(1, ChronoUnit.HOURS))
									   .modifier(userId)
									   .modified(Instant.now())
									   .build();
		try {
			componentService.save(component);
			fail("Saving component with no Acl should have failed");
		} catch (VempainAbstractException e) {
			assertTrue(e.getMessage().contains("ACL ID is invalid"), "Exception message should have contained \"" +
																	 "ACL ID is invalid" + "\": " + e.getMessage());
		} catch (Exception e) {
			fail("We should only have received a VempainAbstractException with invalid ACL ID in component: " + e.getMessage());
		}
	}

	@Test
	void saveWithNoCompNameFail() throws VempainEntityNotFoundException {
		var userId = testITCTools.generateUser();
		long aclId = testITCTools.generateAcl(userId, null, true, true, true, true);
		Component component = Component.builder()
									   .compData("<!-- component data fail -->")
									   .aclId(aclId)
									   .creator(userId)
									   .created(Instant.now().minus(1, ChronoUnit.HOURS))
									   .modifier(userId)
									   .modified(Instant.now())
									   .build();
		try {
			componentService.save(component);
			fail("Saving component with no component name should have failed");
		} catch (VempainComponentException e) {
			assertTrue(e.getMessage().contains("Component name is not set"), "Exception message should have contained \"" +
																			 "Component name is not set" + "\": " + e.getMessage());
		} catch (Exception e) {
			fail("Should have only received a VempainComponentException when component name is invalid: " + e.getMessage());
		} finally {
			testITCTools.deleteAcl(aclId);
		}
	}

	@Test
	void saveWithNoCreatorFail() throws VempainEntityNotFoundException {
		var userId = testITCTools.generateUser();
		var unitId = testITCTools.generateUnit();
		var aclId  = testITCTools.generateAcl(null, unitId, false, true, false, true);
		var component = Component.builder()
								 .compName("Test component Fail")
								 .compData("<!-- component data fail -->")
								 .aclId(aclId)
								 .created(Instant.now().minus(1, ChronoUnit.HOURS))
								 .modifier(userId)
								 .modified(Instant.now())
								 .build();
		try {
			componentService.save(component);
			fail("Saving component with no creator should have failed");
		} catch (VempainAbstractException e) {
			assertTrue(e.getMessage().contains("Creator is missing or invalid"));
		} catch (Exception e) {
			fail("Should have only received a VempainAbstractException when component creator is null: " + e.getMessage());
		} finally {
			log.info("Removing unused ACL ID: {}", aclId);
			testITCTools.deleteAcl(aclId);
		}
	}

	@Test
	void saveWithNoCreatedFail() throws VempainEntityNotFoundException {
		var userId = testITCTools.generateUser();
		var unitId = testITCTools.generateUnit();
		long aclId = testITCTools.generateAcl(null, unitId, false, true, false, false);
		Component component = Component.builder()
									   .compName("Test component Fail")
									   .compData("<!-- component data fail -->")
									   .aclId(aclId)
									   .creator(userId)
									   .modifier(userId)
									   .modified(Instant.now())
									   .build();
		try {
			componentService.save(component);
			fail("Saving component with no created datetime should have failed");
		} catch (VempainAbstractException e) {
			assertTrue(e.getMessage().contains("Created datetime is missing"));
		} catch (Exception e) {
			fail("Should have only received a VempainAbstractException when component creator is null: " + e.getMessage());
		} finally {
			testITCTools.deleteAcl(aclId);
		}
	}

	@Test
	void saveCreatedNewerThanModifiedFail() throws VempainEntityNotFoundException {
		var userId = testITCTools.generateUser();
		var unitId = testITCTools.generateUnit();
		long aclId = testITCTools.generateAcl(null, unitId, false, false, false, true);
		Component component = Component.builder()
									   .compName("Test component Fail")
									   .compData("<!-- component data fail -->")
									   .aclId(aclId)
									   .creator(userId)
									   .created(Instant.now().plus(1, ChronoUnit.HOURS))
									   .modifier(userId)
									   .modified(Instant.now())
									   .build();
		try {
			componentService.save(component);
			fail("Saving component with created timestamp newer than modified should have failed");
		} catch (VempainAbstractException e) {
			assertTrue(e.getMessage().contains("Created datetime is more recent than modified"));
		} catch (Exception e) {
			fail("Should have only received a VempainAbstractException when component created is more recent than modified: " + e.getMessage());
		} finally {
			testITCTools.deleteAcl(aclId);
		}
	}

	@Test
	void saveInvalidCreatorFail() throws VempainEntityNotFoundException {
		var userId = testITCTools.generateUser();
		var unitId = testITCTools.generateUnit();
		long aclId = testITCTools.generateAcl(null, unitId, false, false, true, true);
		Component component = Component.builder()
									   .compName("Test component Fail")
									   .compData("<!-- component data fail -->")
									   .aclId(aclId)
									   .creator(-1L)
									   .created(Instant.now().minus(1, ChronoUnit.HOURS))
									   .modifier(userId)
									   .modified(Instant.now())
									   .build();
		try {
			componentService.save(component);
			fail("Saving component with invalid creator id should have failed");
		} catch (VempainAbstractException e) {
			assertTrue(e.getMessage().contains("Creator is missing or invalid"));
		} catch (Exception e) {
			fail("Should have only received a VempainAbstractException when component creator is invalid: " + e.getMessage());
		} finally {
			testITCTools.deleteAcl(aclId);
		}
	}

	@Test
	void saveInvalidModifierFail() throws VempainEntityNotFoundException {
		var userId = testITCTools.generateUser();
		var unitId = testITCTools.generateUnit();
		long aclId = testITCTools.generateAcl(null, unitId, true, false, true, true);
		Component component = Component.builder()
									   .compName("Test component Fail")
									   .compData("<!-- component data fail -->")
									   .aclId(aclId)
									   .creator(userId)
									   .created(Instant.now().minus(1, ChronoUnit.HOURS))
									   .modifier(-1L)
									   .modified(Instant.now())
									   .build();
		try {
			componentService.save(component);
			fail("Saving component with invalid component modifier id should have failed");
		} catch (VempainAbstractException e) {
			assertTrue(e.getMessage().contains("Entity modifier is invalid"));
		} catch (Exception e) {
			fail("Should have only received a VempainAbstractException when component modifier is invalid: " + e.getMessage());
		} finally {
			testITCTools.deleteAcl(aclId);
		}
	}

	@Test
	void delete() throws VempainComponentException, VempainEntityNotFoundException, VempainAbstractException {
		var userId = testITCTools.generateUser();
		long aclId = testITCTools.generateAcl(userId, null, true, true, true, true);
		Component component = Component.builder()
									   .compName("Component test name")
									   .compData("<!-- component data fail -->")
									   .aclId(aclId)
									   .locked(false)
									   .creator(userId)
									   .created(Instant.now().minus(1, ChronoUnit.HOURS))
									   .modifier(userId)
									   .modified(Instant.now())
									   .build();
		var comp = componentService.save(component);
		assertEquals(aclId, comp.getAclId());

		long componentId = component.getId();
		componentService.deleteById(componentId);

		try {
			Component component1 = componentService.findById(componentId);
			fail("We should not have found a component. Got: " + component1);
		} catch (VempainComponentException e) {
			assertEquals("Failed to find component", e.getMessage());
		}

		Iterable<Acl> acls = aclService.findAclByAclId(aclId);
		assertEquals(0, StreamSupport.stream(acls.spliterator(), false).count());
	}

	private void assertComponent(Component component) {
		assertNotNull(component);
		assertTrue(component.getId() > 0);
		assertNotNull(component.getCompName());
		assertNotNull(component.getCompData());
		assertTrue(component.getAclId() > 0);
		assertNotNull(component.getCreator());
		assertTrue(component.getCreator() > 0);
		assertNotNull(component.getCreated());

		if  (component.getModifier() != null) {
			assertNotNull(component.getModified());
		}
	}
}
