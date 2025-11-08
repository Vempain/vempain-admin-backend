package fi.poltsi.vempain.admin.entity;

import fi.poltsi.vempain.admin.tools.TestUTCTools;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormComponentUTC {

	@Test
	void testEqualsOk() {
		var fc1 = TestUTCTools.generateFormComponent(1L, 1L);
		var fc2 = TestUTCTools.deepCopy(fc1, FormComponent.class);
		var equals = fc1.equals(fc2);
		assertTrue(equals);
	}

	@Test
	void canEqualOk() {
		var fc1 = TestUTCTools.generateFormComponent(1L, 1L);
		var fc2 = TestUTCTools.deepCopy(fc1, FormComponent.class);
		assertTrue(fc1.canEqual(fc2));
	}

	@Test
	void testHashCodeOk() {
		var fc1 = TestUTCTools.generateFormComponent(1L, 1L);
		var fc2 = TestUTCTools.deepCopy(fc1, FormComponent.class);
		assertNotNull(fc2);
		assertEquals(fc1.hashCode(), fc2.hashCode());
	}

	@Test
	void testToStringOk() {
		var fc1 = TestUTCTools.generateFormComponent(1L, 1L);
		assertEquals("FormComponent(sortOrder=1, formId=1, componentId=1)", fc1.toString());
	}

	@Test
	void getComponentIdOk() {
		var fc1 = FormComponent.builder()
							   .sortOrder(1L)
							   .componentId(1L)
							   .formId(1L)
							   .build();
		assertEquals(1L, fc1.getComponentId());
	}

	@Test
	void getFormIdOk() {
		var fc1 = FormComponent.builder()
							   .sortOrder(1L)
							   .componentId(1L)
							   .formId(1L)
							   .build();
		assertEquals(1L, fc1.getFormId());
	}

	@Test
	void getSortOrderOk() {
		var fc1 = FormComponent.builder()
							   .sortOrder(1L)
							   .componentId(1L)
							   .formId(1L)
							   .build();
		assertEquals(1L, fc1.getSortOrder());
	}

	@Test
	void setSortOrderOk() {
		var formComponent = FormComponent.builder()
										 .build();
		formComponent.setSortOrder(1L);
		assertEquals(1L, formComponent.getSortOrder());
	}

	@Test
	void setFormIdOk() {
		var formComponent = FormComponent.builder()
										 .build();
		formComponent.setFormId(1L);
		assertEquals(1L, formComponent.getFormId());
	}

	@Test
	void setComponentOk() {
		var formComponent = FormComponent.builder()
										 .build();
		formComponent.setComponentId(1L);
		assertEquals(1L, formComponent.getComponentId());
	}

	@Test
	void builderOk() {
		var formComponent = FormComponent.builder()
										 .sortOrder(1L)
										 .componentId(1L)
										 .formId(1L)
										 .build();
		assertEquals(1L, formComponent.getComponentId());
		assertEquals(1L, formComponent.getFormId());
		assertEquals(1L, formComponent.getSortOrder());
	}
}
