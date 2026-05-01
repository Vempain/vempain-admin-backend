package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.service.DeleteService;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
class DeleteServiceITC extends AbstractITCTest {

	@Autowired
	private DeleteService deleteService;

	// ---- deleteLayoutById ----

	@Test
	void deleteLayoutByIdOk() throws Exception {
		var layoutIds = testITCTools.generateLayouts(1);
		long layoutId = layoutIds.getFirst();

		try {
			deleteService.deleteLayoutById(layoutId);
		} catch (Exception e) {
			fail("Deleting an existing layout should not throw: " + e.getMessage());
		}

		assertThrows(VempainEntityNotFoundException.class, () -> deleteService.deleteLayoutById(layoutId));
	}

	@Test
	void deleteLayoutByIdNotFoundFail() {
		assertThrows(VempainEntityNotFoundException.class, () -> deleteService.deleteLayoutById(999999L));
	}

	// ---- deleteComponentById ----

	@Test
	void deleteComponentByIdOk() throws Exception {
		var componentIds = testITCTools.generateComponents(1);
		long componentId = componentIds.getFirst();

		try {
			deleteService.deleteComponentById(componentId);
		} catch (Exception e) {
			fail("Deleting an existing component should not throw: " + e.getMessage());
		}

		assertThrows(VempainEntityNotFoundException.class, () -> deleteService.deleteComponentById(componentId));
	}

	@Test
	void deleteComponentByIdNotFoundFail() {
		assertThrows(VempainEntityNotFoundException.class, () -> deleteService.deleteComponentById(999999L));
	}

	// ---- deleteFormById ----

	@Test
	void deleteFormByIdOk() throws Exception {
		var formId = testITCTools.generateForm();

		try {
			deleteService.deleteFormById(formId);
		} catch (Exception e) {
			fail("Deleting an existing form should not throw: " + e.getMessage());
		}

		assertThrows(VempainEntityNotFoundException.class, () -> deleteService.deleteFormById(formId));
	}

	@Test
	void deleteFormByIdNotFoundFail() {
		assertThrows(VempainEntityNotFoundException.class, () -> deleteService.deleteFormById(999999L));
	}

	// ---- deletePageById ----

	@Test
	void deletePageByIdOk() throws Exception {
		var pageId = testITCTools.generatePage();

		try {
			deleteService.deletePageById(pageId);
		} catch (Exception e) {
			fail("Deleting an existing page should not throw: " + e.getMessage());
		}

		assertThrows(VempainEntityNotFoundException.class, () -> deleteService.deletePageById(pageId));
	}

	@Test
	void deletePageByIdNotFoundFail() {
		assertThrows(VempainEntityNotFoundException.class, () -> deleteService.deletePageById(999999L));
	}

	// ---- deleteLayoutById cascades to forms ----

	@Test
	void deleteLayoutByIdCascadesToFormsOk() throws Exception {
		// Create a layout, then a form referencing it
		var layoutId = testITCTools.generateLayout();
		long userId = testITCTools.generateUser();
		long aclId = testITCTools.generateAcl(userId, null, true, true, true, true);

		var form = fi.poltsi.vempain.admin.entity.Form.builder()
													  .formName("Cascade test form")
													  .layoutId(layoutId)
													  .aclId(aclId)
													  .locked(false)
													  .creator(userId)
													  .created(java.time.Instant.now())
													  .build();
		formRepository.save(form);

		// Deleting the layout should cascade-delete the form
		try {
			deleteService.deleteLayoutById(layoutId);
		} catch (Exception e) {
			fail("Deleting layout should not throw even with child forms: " + e.getMessage());
		}
	}
}
