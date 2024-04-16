package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.response.FormResponse;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
class FormServiceITC extends AbstractITCTest {
	private final long initCount = 10L;

	@AfterEach
	void tearDown() throws ProcessingFailedException {
		testITCTools.deleteForms();
		testITCTools.deleteAcls();
	}

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
		testITCTools.generateForms(initCount);

		try {
			Form form = formService.findById(testITCTools.getFormIdList().get(0));
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
		testITCTools.generateForms(initCount);

		try {
			formService.delete(testITCTools.getFormIdList().get(0));
			testITCTools.getFormIdList().remove(0);
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
		testITCTools.getFormIdList().clear();
		testITCTools.generateFormComponents(4L, 4L);

		try {
			Form form = formService.findById(testITCTools.getFormIdList().get(1));
			FormResponse formResponse = formService.getFormResponse(form);
			assertNotNull(formResponse);
		} catch (Exception e) {
			fail("Failed to create form response from form: " + e.getMessage());
		}
	}
}
