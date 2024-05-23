package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.response.FormResponse;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
class FormServiceITC extends AbstractITCTest {
	private final long initCount = 10L;

	@Test
	void findAll() {
		Iterable<Form> forms = formService.findAll();
		assertNotNull(forms);

		for (Form form : forms) {
			testITCTools.assertForm(form);
		}
	}

	@Test
	void findById() {
		var formIds = testITCTools.generateForms(initCount);

		try {
			Form form = formService.findById(formIds.getFirst());
			testITCTools.assertForm(form);
		} catch (VempainEntityNotFoundException e) {
			fail("There should have been a form to be found");
		}
	}

	@Test
	void findByFormName() {
		testITCTools.generateForms(initCount);

		try {
			Form form = formService.findByFormName("Test form 1");
			testITCTools.assertForm(form);
		} catch (VempainEntityNotFoundException e) {
			fail("Failed to fetch form by name");
		}
	}

	@Test
	void deleteById() {
		var formIds = testITCTools.generateForms(initCount);

		try {
			for (Long formId : formIds) {
				formService.delete(formId);
			}
		} catch (Exception e) {
			fail("Failed to remove form by ID");
		}
	}

	@Test
	void save() {
		var userId = testITCTools.generateUser();
		var aclId = testITCTools.generateAcl(userId, null, true, true, true, true);
		var layoutId = testITCTools.generateLayout();
		Form form = Form.builder()
						.formName("CreateForm")
						.layoutId(layoutId)
						.aclId(aclId)
						.locked(false)
						.creator(userId)
						.created(Instant.now().minus(1, ChronoUnit.HOURS))
						.modifier(null)
						.modified(null)
						.build();

		try {
			formService.save(form);
		} catch (Exception e) {
			fail("Failed to create a valid form");
		}
	}

	@Test
	void getFormResponseOk() throws VempainComponentException, VempainAbstractException, VempainEntityNotFoundException {
		var formComponentList = testITCTools.generateFormComponents(4L, 4L);

		try {
			for (var formComponent : formComponentList) {
				Form         form         = formService.findById(formComponent.keySet().iterator().next());
				FormResponse formResponse = formService.getFormResponse(form);
				assertNotNull(formResponse);
			}
		} catch (Exception e) {
			fail("Failed to create form response from form: " + e.getMessage());
		}
	}
}
