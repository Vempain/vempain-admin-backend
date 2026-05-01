package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.entity.FormComponent;
import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.repository.ComponentRepository;
import fi.poltsi.vempain.admin.repository.FormRepository;
import fi.poltsi.vempain.admin.repository.LayoutRepository;
import fi.poltsi.vempain.admin.repository.PageRepository;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.repository.AclRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteServiceUTC {

	@Mock
	private ComponentRepository  componentRepository;
	@Mock
	private AclRepository        aclRepository;
	@Mock
	private FormComponentService formComponentService;
	@Mock
	private PageRepository       pageRepository;
	@Mock
	private FormRepository       formRepository;
	@Mock
	private LayoutRepository     layoutRepository;

	@InjectMocks
	private DeleteService deleteService;

	// ---- deleteLayoutById ----

	@Test
	void deleteLayoutByIdOk() throws VempainEntityNotFoundException {
		Layout layout = TestUTCTools.generateLayout(1L);
		when(layoutRepository.findById(1L)).thenReturn(Optional.of(layout));
		when(formRepository.findByLayoutId(1L)).thenReturn(Collections.emptyList());
		doNothing().when(aclRepository).deleteAclsByAclId(anyLong());
		doNothing().when(layoutRepository).deleteById(anyLong());

		try {
			deleteService.deleteLayoutById(1L);
		} catch (Exception e) {
			fail("Should not throw: " + e.getMessage());
		}

		verify(layoutRepository).deleteById(1L);
		verify(aclRepository).deleteAclsByAclId(layout.getAclId());
	}

	@Test
	void deleteLayoutByIdWithFormsOk() throws VempainEntityNotFoundException {
		Layout layout = TestUTCTools.generateLayout(1L);
		Form form = TestUTCTools.generateForm(10L, 10L);
		when(layoutRepository.findById(1L)).thenReturn(Optional.of(layout));
		when(formRepository.findByLayoutId(1L)).thenReturn(List.of(form));
		when(formRepository.findById(10L)).thenReturn(Optional.of(form));
		when(pageRepository.findByFormId(10L)).thenReturn(Collections.emptyList());
		when(formComponentService.findFormComponentByFormId(10L)).thenReturn(Collections.emptyList());
		doNothing().when(aclRepository).deleteAclsByAclId(anyLong());
		doNothing().when(formRepository).deleteById(anyLong());
		doNothing().when(layoutRepository).deleteById(anyLong());

		try {
			deleteService.deleteLayoutById(1L);
		} catch (Exception e) {
			fail("Should not throw: " + e.getMessage());
		}

		verify(layoutRepository).deleteById(1L);
		verify(formRepository).deleteById(10L);
	}

	@Test
	void deleteLayoutByIdNotFoundFail() {
		when(layoutRepository.findById(99L)).thenReturn(Optional.empty());
		assertThrows(VempainEntityNotFoundException.class, () -> deleteService.deleteLayoutById(99L));
		verify(layoutRepository, never()).deleteById(anyLong());
	}

	// ---- deleteComponentById ----

	@Test
	void deleteComponentByIdOk() throws VempainEntityNotFoundException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		when(componentRepository.findById(1L)).thenReturn(Optional.of(component));
		when(formComponentService.findFormComponentByComponentId(1L)).thenReturn(Collections.emptyList());
		doNothing().when(aclRepository).deleteAclsByAclId(anyLong());
		doNothing().when(componentRepository).deleteById(anyLong());

		try {
			deleteService.deleteComponentById(1L);
		} catch (Exception e) {
			fail("Should not throw: " + e.getMessage());
		}

		verify(componentRepository).deleteById(1L);
		verify(aclRepository).deleteAclsByAclId(component.getAclId());
	}

	@Test
	void deleteComponentByIdWithFormsOk() throws VempainEntityNotFoundException {
		Component component = TestUTCTools.generateComponent(1L, 1L);
		FormComponent fc = TestUTCTools.generateFormComponent(5L, 1L);
		Form form = TestUTCTools.generateForm(5L, 5L);
		when(componentRepository.findById(1L)).thenReturn(Optional.of(component));
		when(formComponentService.findFormComponentByComponentId(1L)).thenReturn(List.of(fc));
		when(formRepository.findById(5L)).thenReturn(Optional.of(form));
		when(pageRepository.findByFormId(5L)).thenReturn(Collections.emptyList());
		when(formComponentService.findFormComponentByFormId(5L)).thenReturn(Collections.emptyList());
		doNothing().when(aclRepository).deleteAclsByAclId(anyLong());
		doNothing().when(formRepository).deleteById(anyLong());
		doNothing().when(componentRepository).deleteById(anyLong());

		try {
			deleteService.deleteComponentById(1L);
		} catch (Exception e) {
			fail("Should not throw: " + e.getMessage());
		}

		verify(componentRepository).deleteById(1L);
	}

	@Test
	void deleteComponentByIdNotFoundFail() {
		when(componentRepository.findById(99L)).thenReturn(Optional.empty());
		assertThrows(VempainEntityNotFoundException.class, () -> deleteService.deleteComponentById(99L));
		verify(componentRepository, never()).deleteById(anyLong());
	}

	// ---- deleteFormById ----

	@Test
	void deleteFormByIdOk() throws VempainEntityNotFoundException {
		Form form = TestUTCTools.generateForm(2L, 2L);
		when(formRepository.findById(2L)).thenReturn(Optional.of(form));
		when(pageRepository.findByFormId(2L)).thenReturn(Collections.emptyList());
		when(formComponentService.findFormComponentByFormId(2L)).thenReturn(Collections.emptyList());
		doNothing().when(aclRepository).deleteAclsByAclId(anyLong());
		doNothing().when(formRepository).deleteById(anyLong());

		try {
			deleteService.deleteFormById(2L);
		} catch (Exception e) {
			fail("Should not throw: " + e.getMessage());
		}

		verify(formRepository).deleteById(2L);
		verify(aclRepository).deleteAclsByAclId(form.getAclId());
	}

	@Test
	void deleteFormByIdWithPagesOk() throws VempainEntityNotFoundException {
		Form form = TestUTCTools.generateForm(2L, 2L);
		Page page = TestUTCTools.generatePage(7L);
		when(formRepository.findById(2L)).thenReturn(Optional.of(form));
		when(pageRepository.findByFormId(2L)).thenReturn(List.of(page));
		when(pageRepository.findById(7L)).thenReturn(page);
		when(formComponentService.findFormComponentByFormId(2L)).thenReturn(Collections.emptyList());
		doNothing().when(aclRepository).deleteAclsByAclId(anyLong());
		doNothing().when(formRepository).deleteById(anyLong());
		doNothing().when(pageRepository).deletePageById(anyLong());

		try {
			deleteService.deleteFormById(2L);
		} catch (Exception e) {
			fail("Should not throw: " + e.getMessage());
		}

		verify(pageRepository).deletePageById(7L);
		verify(formRepository).deleteById(2L);
	}

	@Test
	void deleteFormByIdNotFoundFail() {
		when(formRepository.findById(99L)).thenReturn(Optional.empty());
		assertThrows(VempainEntityNotFoundException.class, () -> deleteService.deleteFormById(99L));
		verify(formRepository, never()).deleteById(anyLong());
	}

	// ---- deletePageById ----

	@Test
	void deletePageByIdOk() throws VempainEntityNotFoundException {
		Page page = TestUTCTools.generatePage(3L);
		when(pageRepository.findById(3L)).thenReturn(page);
		doNothing().when(aclRepository).deleteAclsByAclId(anyLong());
		doNothing().when(pageRepository).deletePageById(anyLong());

		try {
			deleteService.deletePageById(3L);
		} catch (Exception e) {
			fail("Should not throw: " + e.getMessage());
		}

		verify(pageRepository).deletePageById(3L);
		verify(aclRepository).deleteAclsByAclId(page.getAclId());
	}

	@Test
	void deletePageByIdNotFoundFail() {
		when(pageRepository.findById(99L)).thenReturn(null);
		assertThrows(VempainEntityNotFoundException.class, () -> deleteService.deletePageById(99L));
		verify(pageRepository, never()).deletePageById(anyLong());
	}
}
