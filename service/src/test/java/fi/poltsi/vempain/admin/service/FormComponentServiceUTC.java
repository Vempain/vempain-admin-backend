package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.entity.FormComponent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class FormComponentServiceUTC {

	@Mock
	private EntityManager entityManager;
	@Mock
	private Query         query;

	@InjectMocks
	private FormComponentService formComponentService;

	@BeforeEach
	void setUp() {
		when(entityManager.createNativeQuery(anyString())).thenReturn(query);
		when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
	}

	// ---- deleteFormComponent ----

	@Test
	void deleteFormComponentOk() {
		when(query.executeUpdate()).thenReturn(1);

		formComponentService.deleteFormComponent(1L, 2L, 0L);

		verify(query).executeUpdate();
	}

	@Test
	void deleteFormComponentNoMatchOk() {
		when(query.executeUpdate()).thenReturn(0);

		formComponentService.deleteFormComponent(99L, 99L, 99L);

		verify(query).executeUpdate();
	}

	// ---- findFormComponentByFormId ----

	@Test
	void findFormComponentByFormIdOk() {
		Object[] row = {1L, 2L, 0L};
		when(query.getResultList()).thenReturn(Collections.singletonList(row));

		List<FormComponent> result = formComponentService.findFormComponentByFormId(1L);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(1L, result.getFirst().getFormId());
		assertEquals(2L, result.getFirst().getComponentId());
		assertEquals(0L, result.getFirst().getSortOrder());
	}

	@Test
	void findFormComponentByFormIdEmptyOk() {
		when(query.getResultList()).thenReturn(Collections.emptyList());

		List<FormComponent> result = formComponentService.findFormComponentByFormId(99L);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	// ---- findFormComponentByComponentId ----

	@Test
	void findFormComponentByComponentIdOk() {
		Object[] row = {3L, 5L, 1L};
		when(query.getResultList()).thenReturn(Collections.singletonList(row));

		List<FormComponent> result = formComponentService.findFormComponentByComponentId(5L);

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(3L, result.getFirst().getFormId());
		assertEquals(5L, result.getFirst().getComponentId());
		assertEquals(1L, result.getFirst().getSortOrder());
	}

	@Test
	void findFormComponentByComponentIdEmptyOk() {
		when(query.getResultList()).thenReturn(Collections.emptyList());

		List<FormComponent> result = formComponentService.findFormComponentByComponentId(999L);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void findFormComponentByComponentIdMultipleRowsOk() {
		Object[] row1 = {1L, 5L, 0L};
		Object[] row2 = {2L, 5L, 1L};
		when(query.getResultList()).thenReturn(List.of(row1, row2));

		List<FormComponent> result = formComponentService.findFormComponentByComponentId(5L);

		assertNotNull(result);
		assertEquals(2, result.size());
	}

	// ---- addFormComponent ----

	@Test
	void addFormComponentOk() {
		when(query.executeUpdate()).thenReturn(1);

		formComponentService.addFormComponent(1L, 2L, 0L);

		verify(query).executeUpdate();
	}
}
