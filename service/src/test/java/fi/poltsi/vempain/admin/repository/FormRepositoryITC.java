package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class FormRepositoryITC extends AbstractITCTest {
	private final long initCount = 10;

	@AfterEach
	void tearDown() throws ProcessingFailedException {
		testITCTools.deleteForms();
		testITCTools.checkDatabase();
	}

	@Test
	void findAll() {
		testITCTools.generateForms(initCount);
		var forms = formRepository.findAll();
		assertNotNull(forms);
		assertEquals(initCount, StreamSupport.stream(forms.spliterator(), false).count());

		for (Form form : forms) {
			assertForm(form);
		}
	}

	@Test
	void findByIdOk() {
		testITCTools.generateForms(initCount);
		var optionalForm = formRepository.findById(testITCTools.getFormIdList().get(1));
		assertTrue(optionalForm.isPresent());
		assertForm(optionalForm.get());
	}

	@Test
	void findByFormNameOk() {
		testITCTools.generateForms(initCount);
		var form = formRepository.findByFormName("Test form 1");
		assertForm(form);
	}

	@Transactional
	@Test
	void deleteFormById() {
		testITCTools.generateForms(initCount);
		var forms = formRepository.findAll();
		assertTrue(StreamSupport.stream(forms.spliterator(), false).findAny().isPresent());
		var form = forms.iterator().next();
		formRepository.deleteById(form.getId());
		Optional<Form> noForm = formRepository.findById(form.getId());
		assertTrue(noForm.isEmpty());
	}

	@Test
	void findComponentByFormIdOk() throws VempainComponentException, VempainAbstractException {
		var formId = testITCTools.generateForm();
		assertNotNull(formId);
		var componentIdList = testITCTools.generateComponents(3);
		testITCTools.addComponentsToForm(formId, componentIdList);
		var components = formComponentService.findFormComponentByFormId(formId);
		assertEquals(3, components.size());
	}

	private void assertForm(Form form) {
		assertNotNull(form);
		assertTrue(form.getCreator() > 0);
		assertNotNull(form.getCreated());
		assertNull(form.getModifier());
		assertNull(form.getModified());
		assertTrue(form.getId() > 0);
		assertTrue(form.getFormName().contains("Test form "));
	}
}
