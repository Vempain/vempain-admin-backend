package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.entity.FormComponent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class FormComponentServiceITC extends AbstractITCTest {

	// ---- addFormComponent ----

	@Test
	void addFormComponentOk() throws Exception {
		long formId = testITCTools.generateForm();
		var componentIds = testITCTools.generateComponents(1);
		long componentId = componentIds.getFirst();

		formComponentService.addFormComponent(formId, componentId, 0L);

		List<FormComponent> result = formComponentService.findFormComponentByFormId(formId);
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(formId, result.getFirst().getFormId());
		assertEquals(componentId, result.getFirst().getComponentId());
	}

	// ---- findFormComponentByFormId ----

	@Test
	void findFormComponentByFormIdOk() throws Exception {
		var formComponents = testITCTools.generateFormComponents(1, 2);
		assertNotNull(formComponents);
		assertEquals(1, formComponents.size());

		Map<Long, List<Long>> formComponentMap = formComponents.getFirst();
		long formId = formComponentMap.keySet().iterator().next();
		List<Long> componentIds = formComponentMap.get(formId);
		assertEquals(2, componentIds.size());

		List<FormComponent> result = formComponentService.findFormComponentByFormId(formId);
		assertNotNull(result);
		assertEquals(2, result.size());
	}

	@Test
	void findFormComponentByFormIdNotFoundOk() {
		List<FormComponent> result = formComponentService.findFormComponentByFormId(999999L);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	// ---- findFormComponentByComponentId ----

	@Test
	void findFormComponentByComponentIdOk() throws Exception {
		var formComponents = testITCTools.generateFormComponents(1, 1);
		Map<Long, List<Long>> formComponentMap = formComponents.getFirst();
		long formId = formComponentMap.keySet().iterator().next();
		long componentId = formComponentMap.get(formId).getFirst();

		List<FormComponent> result = formComponentService.findFormComponentByComponentId(componentId);
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(componentId, result.getFirst().getComponentId());
	}

	@Test
	void findFormComponentByComponentIdNotFoundOk() {
		List<FormComponent> result = formComponentService.findFormComponentByComponentId(999999L);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	// ---- deleteFormComponent ----

	@Test
	void deleteFormComponentOk() throws Exception {
		long formId = testITCTools.generateForm();
		var componentIds = testITCTools.generateComponents(1);
		long componentId = componentIds.getFirst();
		formComponentService.addFormComponent(formId, componentId, 0L);

		List<FormComponent> before = formComponentService.findFormComponentByFormId(formId);
		assertEquals(1, before.size());

		formComponentService.deleteFormComponent(formId, componentId, 0L);

		List<FormComponent> after = formComponentService.findFormComponentByFormId(formId);
		assertTrue(after.isEmpty());
	}
}
