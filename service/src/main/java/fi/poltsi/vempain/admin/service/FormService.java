package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.request.ComponentRequest;
import fi.poltsi.vempain.admin.api.request.FormRequest;
import fi.poltsi.vempain.admin.api.response.AclResponse;
import fi.poltsi.vempain.admin.api.response.ComponentResponse;
import fi.poltsi.vempain.admin.api.response.FormResponse;
import fi.poltsi.vempain.admin.entity.Acl;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.entity.Form;
import fi.poltsi.vempain.admin.entity.FormComponent;
import fi.poltsi.vempain.admin.exception.EntityAlreadyExistsException;
import fi.poltsi.vempain.admin.exception.InvalidRequestException;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.repository.FormRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FormService extends AbstractService {
	private final FormRepository       formRepository;
	private final ComponentService     componentService;
	private final FormComponentService formComponentService;

	public FormService(AclService aclService, FormRepository formRepository, ComponentService componentService,
					   AccessService accessService, FormComponentService formComponentService) {
		super(aclService, accessService);
		this.formRepository       = formRepository;
		this.componentService     = componentService;
		this.formComponentService = formComponentService;
	}


	public Iterable<Form> findAll() {
		return formRepository.findAll();
	}

	public List<FormResponse> findAllAsResponsesForUser(QueryDetailEnum requestForm) throws VempainComponentException {
		var forms     = findAll();
		var responses = new ArrayList<FormResponse>();

		for (Form form : forms) {
			if (accessService.hasReadPermission(form.getAclId())) {
				responses.add(getFormResponse(form));
			}
		}

		return responses;
	}

	public Form findById(long formId) throws VempainEntityNotFoundException {
		var optionalForm = formRepository.findById(formId);

		if (optionalForm.isEmpty()) {
			log.error("Unable to find a form with ID {}", formId);
			throw new VempainEntityNotFoundException("Failed to find form by name", "form");
		}

		return optionalForm.get();
	}


	public FormResponse getFormResponseById(Long formId) throws VempainEntityNotFoundException, VempainComponentException {
		var form = findById(formId);
		return getFormResponse(form);
	}

	public FormResponse findByIdForUser(long formId) throws VempainComponentException, VempainEntityNotFoundException {
		var userId = getUserId();

		var form = findById(formId);

		if (accessService.hasReadPermission(form.getAclId())) {
			return getFormResponse(form);
		} else {
			log.error("User {} tried to access component {} ({}) with insufficient permissions", userId, form.getId(),
					  form.getFormName());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.UNAUTHORIZED_ACCESS);
		}
	}

	private boolean isFormNameTaken(String formName) {
		var form = formRepository.findByFormName(formName);
		return form != null;
	}

	public Form findByFormName(String formName) throws VempainEntityNotFoundException {
		var form = formRepository.findByFormName(formName);

		if (form == null) {
			log.error("Could not find a form with the name: {}", formName);
			throw new VempainEntityNotFoundException("Failed to find form by name", "form");
		}

		return form;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void delete(long formId) throws VempainEntityNotFoundException, ProcessingFailedException {
		var optionalForm = formRepository.findById(formId);

		if (optionalForm.isEmpty()) {
			log.error("Attempted to delete non-existing form ID {}", formId);
			throw new VempainEntityNotFoundException("No form can be found for deletion", "form");
		}

		var form = optionalForm.get();

		if (!accessService.hasDeletePermission(form.getAclId())) {
			log.error("User tried to delete form ID {} without proper access", formId);
			throw new AccessDeniedException("User does not have permission to delete form");
		}

		// Delete also the Acl
		try {
			log.info("Form ACL ID: {}", form.getAclId());
			aclService.deleteByAclId(form.getAclId());
		} catch (Exception e) {
			log.error("Failed to remove acl: {}", form.getAclId(), e);
		}

		var formComponents = formComponentService.findFormComponentByFormId(formId);
		log.info("Deleting formComponents: {}", formComponents);

		for (FormComponent formComponent : formComponents) {
			formComponentService.deleteFormComponent(formComponent.getFormId(), formComponent.getComponentId(), formComponent.getSortOrder());
		}

		try {
			log.info("Deleting form ID: {}", formId);
			formRepository.deleteById(formId);
		} catch (Exception e) {
			log.error("Failed to delete form: {}", form, e);
			throw new ProcessingFailedException("Unknown exception when deleting form");
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Form save(Form form) {
		return formRepository.save(form);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public FormResponse saveRequest(FormRequest formRequest) throws EntityAlreadyExistsException, InvalidRequestException, ProcessingFailedException {

		if (isFormNameTaken(formRequest.getName().trim())) {
			log.error("Form name {} already exists", formRequest.getName());
			throw new EntityAlreadyExistsException("Tried to save a form with an existing name", "form");
		}

		if (formRequest.getComponents() == null || formRequest.getComponents().isEmpty()) {
			log.error("Missing component list in form request: {}", formRequest);
			throw new InvalidRequestException("Missing component list in form request");
		}

		long aclId = aclService.getNextAclId();

		try {
			aclService.saveAclRequests(aclId, formRequest.getAcls());
		} catch (VempainAclException ex) {
			log.error("Error saving ACLs for new form: {}", formRequest);
			throw new InvalidRequestException("Failed to save the ACLs of request");
		}

		Form form = Form.builder()
						.formName(formRequest.getName())
						.layoutId(formRequest.getLayoutId())
						.aclId(aclId)
						.creator(getUserId())
						.created(Instant.now())
						.build();

		var newForm = save(form);

		saveFormComponents(formRequest.getComponents(), newForm);

		try {
			return getFormResponse(newForm);
		} catch (VempainComponentException componentException) {
			log.error("Failed to retrieve the saved form request {}", formRequest);
			throw new ProcessingFailedException("Failed to retrieve the saved form");
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public FormResponse updateForm(FormRequest formRequest) throws VempainEntityNotFoundException, EntityAlreadyExistsException, InvalidRequestException, ProcessingFailedException {
		// First get the current version of the form
		Form form;

		try {
			form = findById(formRequest.getId());
		} catch (VempainEntityNotFoundException e) {
			log.error("Failed to find form with ID {}", formRequest.getId());
			throw new VempainEntityNotFoundException("Failed to find form with ID", "form");
		}

		if (!accessService.hasModifyPermission(form.getAclId())) {
			log.error("User tried to modify form ID {} without proper access", formRequest.getId());
			throw new AccessDeniedException("User does not have permission to modify form");
		}

		if (formRequest.getComponents() == null || formRequest.getComponents().isEmpty()) {
			log.error("Missing component list in form request: {}", formRequest);
			throw new InvalidRequestException("Missing component list in form request");
		}

		// If the name has changed, check whether the new name is unique
		if (!form.getFormName().equals(formRequest.getName().trim())) {
			if (isFormNameTaken(formRequest.getName())) {
				log.error("Form name {} already exists", formRequest.getName());
				throw new EntityAlreadyExistsException("Tried to save a form with an existing name", "form");
			}
		}

		try {
			aclService.saveAclRequests(form.getAclId(), formRequest.getAcls());
		} catch (VempainAclException ex) {
			log.error("Error saving ACLs for new form: {}", formRequest);
			throw new InvalidRequestException("Failed to save the ACLs of request");
		}

		form.setFormName(formRequest.getName());
		form.setLayoutId(formRequest.getLayoutId());
		form.setModified(Instant.now());
		form.setModifier(getUserId());
		var returnForm = save(form);

		saveFormComponents(formRequest.getComponents(), returnForm);
		try {
			return getFormResponse(returnForm);
		} catch (VempainComponentException componentException) {
			log.error("Failed to retrieve the saved form request {}", formRequest);
			throw new ProcessingFailedException("Failed to retrieve the saved form");
		}
	}

	protected void saveFormComponents(List<ComponentRequest> componentRequests, Form form) throws InvalidRequestException {
		var index = 0L;

		var existingFormComponents = formComponentService.findFormComponentByFormId(form.getId());

		if (existingFormComponents.isEmpty()) {
			// This is a new form, just add the components
			for (var componentRequest : componentRequests) {
				if (componentRequest == null || componentRequest.getId() < 1) {
					log.error("Malformed form request, one of the components is null");
					throw new InvalidRequestException("Malformed component ID in form request");
				}

				Component component;

				try {
					component = componentService.findById(componentRequest.getId());
				} catch (VempainComponentException componentException) {
					log.error("Form request refers to a non-existing component ID: {}", componentRequest);
					throw new InvalidRequestException("Malformed component ID in form request");
				}

				formComponentService.addFormComponent(form.getId(), component.getId(), index);
				index++;
			}
		} else {
			// Remove the old and then add the new
			for (var existingFormComponent : existingFormComponents) {
				formComponentService.deleteFormComponent(existingFormComponent.getFormId(), existingFormComponent.getComponentId(), existingFormComponent.getSortOrder());
			}

			saveFormComponents(componentRequests, form);
		}
	}

	protected FormResponse getFormResponse(Form form) throws VempainComponentException {
		var response = form.getFormResponse();

		response.setAcls(aclService.getAclResponses(form.getAclId()));

		var formComponents     = formComponentService.findFormComponentByFormId(form.getId());
		var componentResponses = new ArrayList<ComponentResponse>();

		for (FormComponent formComponent : formComponents) {
			var component         = componentService.findById(formComponent.getComponentId());
			var compAcls          = aclService.findAclByAclId(component.getAclId());
			var aclCompResponses  = new ArrayList<AclResponse>();
			var componentResponse = component.getComponentResponse();

			for (Acl acl : compAcls) {
				aclCompResponses.add(acl.toResponse());
			}

			componentResponse.setAcls(aclCompResponses);
			componentResponses.add(componentResponse);
		}

		response.setComponents(componentResponses);
		return response;
	}

	public List<FormComponent> findAllFormComponentsByFormId(long formId) {
		return formComponentService.findFormComponentByFormId(formId);
	}

	public List<FormResponse> findFormsByComponentId(long componentId) {
		var formComponents = formComponentService.findFormComponentByComponentId(componentId);
		var formResponses  = new ArrayList<FormResponse>();

		for (FormComponent formComponent : formComponents) {
			var form = formRepository.findById(formComponent.getFormId());

			if (form.isPresent()) {
				try {
					formResponses.add(getFormResponse(form.get()));
				} catch (VempainComponentException e) {
					log.error("Failed to get form response for form ID {}", formComponent.getFormId(), e);
				}
			}
		}

		return formResponses;
	}

	public List<FormResponse> findFormsByLayoutId(long layoutId) {
		var forms = formRepository.findByLayoutId(layoutId);
		var responses = new ArrayList<FormResponse>();

		for (Form form : forms) {
			try {
				responses.add(getFormResponse(form));
			} catch (VempainComponentException e) {
				log.error("Failed to get form response for form ID {}", form.getId(), e);
			}
		}

		return responses;
	}
}
