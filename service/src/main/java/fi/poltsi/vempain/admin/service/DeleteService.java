package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.repository.AclRepository;
import fi.poltsi.vempain.admin.repository.ComponentRepository;
import fi.poltsi.vempain.admin.repository.FormRepository;
import fi.poltsi.vempain.admin.repository.LayoutRepository;
import fi.poltsi.vempain.admin.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for deleting entities within a single transaction as we need to cascade removal of entities which have foreign keys
 * This would not be needed if the database would be set to CASCADE DELETE
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class DeleteService {
	private final ComponentRepository  componentRepository;
	private final AclRepository        aclRepository;
	private final FormComponentService formComponentService;
	private final PageRepository       pageRepository;
	private final FormRepository       formRepository;
	private final LayoutRepository     layoutRepository;

	/**
	 * Delete a layout as well as any forms and pages that are associated with the layout
	 */
	@Transactional
	public void deleteLayoutById(long layoutId) throws VempainEntityNotFoundException {
		var optionalLayout = layoutRepository.findById(layoutId);

		if (optionalLayout.isEmpty()) {
			log.error("Layout not found: {}", layoutId);
			throw new VempainEntityNotFoundException("Layout not found by id: " + layoutId, "layout");
		}

		var layout = optionalLayout.get();
		var forms  = formRepository.findByLayoutId(layoutId);

		for (var form : forms) {
			deleteFormById(form.getId());
		}

		aclRepository.deleteAclsByAclId(layout.getAclId());
		layoutRepository.deleteById(layoutId);
	}

	/**
	 * Deletion of a component will also remove any and all forms that are associated with the component and as an extension, any and
	 * all pages that are associated with the forms.
	 *
	 * @param componentId Component to be deleted
	 */
	@Transactional
	public void deleteComponentById(long componentId) throws VempainEntityNotFoundException {
		var optionalComponent = componentRepository.findById(componentId);

		if (optionalComponent.isEmpty()) {
			log.error("Component not found: {}", componentId);
			throw new VempainEntityNotFoundException("Component not found by id: " + componentId, "component");
		}

		// Remove the ACLs
		var component = optionalComponent.get();

		// Remove the forms
		var formComponents = formComponentService.findFormComponentByComponentId(componentId);

		for (var formComponent : formComponents) {
			deleteFormById(formComponent.getFormId());
		}

		aclRepository.deleteAclsByAclId(component.getAclId());
		componentRepository.deleteById(componentId);
	}

	/**
	 * Delete a form and its ACL entries, this will also delete any and all pages that are associated with the form
	 *
	 * @param formId Form to be deleted
	 */
	@Transactional
	public void deleteFormById(long formId) throws VempainEntityNotFoundException {
		var optionalForm = formRepository.findById(formId);

		if (optionalForm.isEmpty()) {
			log.error("Form not found: {}", formId);
			throw new VempainEntityNotFoundException("Form not found by id: " + formId, "page");
		}

		var form = optionalForm.get();

		var pages = pageRepository.findByFormId(formId);

		for (var page : pages) {
			deletePageById(page.getId());
		}

		// Remove any form-component associations
		// Remove the forms
		var formComponents = formComponentService.findFormComponentByFormId(formId);

		for (var formComponent : formComponents) {
			formComponentService.deleteFormComponent(formId, formComponent.getComponentId(), formComponent.getSortOrder());
		}

		aclRepository.deleteAclsByAclId(form.getAclId());
		formRepository.deleteById(formId);
	}

	/**
	 * Delete a page and its ACL entries
	 *
	 * @param pageId Page to be deleted
	 */
	@Transactional
	public void deletePageById(long pageId) throws VempainEntityNotFoundException {
		var page = pageRepository.findById(pageId);

		if (page == null) {
			log.error("Page not found: {}", pageId);
			throw new VempainEntityNotFoundException("Page not found by id: " + pageId, "page");
		}

		aclRepository.deleteAclsByAclId(page.getAclId());
		pageRepository.deletePageById(pageId);
	}
}
