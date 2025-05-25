package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.ComponentRequest;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.repository.ComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ComponentService {
	private static final String              COMPONENT_ID = "component";
	private final        ComponentRepository componentRepository;
	private final        AclService          aclService;
	private final        AccessService       accessService;

	public List<Component> findAll() {
		ArrayList<Component> components = new ArrayList<>();

		for (Component component : componentRepository.findAll()) {
			components.add(component);
		}

		return components;
	}

	public List<Component> findAllByUser() {
		Iterable<Component> allComponents = componentRepository.findAll();
		ArrayList<Component> accessableComponents = new ArrayList<>();

		for (Component component : allComponents) {
			if (accessService.hasReadPermission(component.getAclId())) {
				log.info("XXXXXXXXXXXXXXXX Adding {}", component);
				accessableComponents.add(component);
			}
		}

		return accessableComponents;
	}

	public Component findById(long componentId) throws VempainComponentException {
		var optionalComponent = componentRepository.findById(componentId);

		if (optionalComponent.isEmpty()) {
			log.error("Failed to find component by ID: {}", componentId);
			throw new VempainComponentException("Failed to find component");
		}

		return optionalComponent.get();
	}

	public Component findByIdByUser(long componentId) {
		var userId = accessService.getValidUserId();

		Component component;

		try {
			component = findById(componentId);
		} catch (VempainComponentException e) {
			log.error("Failed to fetch component by ID: {}", componentId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}

		if (accessService.hasReadPermission(component.getAclId())) {
			return component;
		} else {
			log.error("User {} tried to access component {} ({}) with insufficient permissions", userId, component.getId(),
					  component.getCompName());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.UNAUTHORIZED_ACCESS);
		}
	}

	public Component findByName(String compName) throws VempainComponentException {
		var component = componentRepository.findByCompName(compName);

		if (component == null) {
			log.error("Could not find a component with the name: {}", compName);
			throw new VempainComponentException("Failed to find component by name");
		}

		return component;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteById(long componentId) throws VempainEntityNotFoundException {
		var optionalComponent = componentRepository.findById(componentId);

		if (optionalComponent.isEmpty()) {
			log.error("Tried to delete a non-existing component ID: {}", componentId);
			throw new VempainEntityNotFoundException("Component not found for deletion", COMPONENT_ID);
		}

		aclService.deleteByAclId(optionalComponent.get()
												  .getAclId());
		componentRepository.deleteById(componentId);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Component save(Component component) throws VempainComponentException, VempainAbstractException {
		validateComponent(component);
		return componentRepository.save(component);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Component saveFromRequest(ComponentRequest request) {
		var userId = accessService.getValidUserId();

		try {
			var component = findByName(request.getCompName());
			log.error("User tried to save a new component with an existing name: {} (ID: {})", component.getCompName(), component.getId());
			throw new ResponseStatusException(HttpStatus.CONFLICT, VempainMessages.OBJECT_NAME_ALREADY_EXISTS);
		} catch (VempainComponentException e) {
			log.info("No component with name {} found, can save it", request.getCompName());
		}

		long aclId = aclService.saveNewAclForObject(request.getAcls());

		Component newComponent = Component.builder()
										  .compData(request.getCompData())
										  .compName(request.getCompName())
										  .locked(false)
										  .aclId(aclId)
										  .creator(userId)
										  .created(Instant.now())
										  .modifier(userId)
										  .modified(Instant.now()
														   .plus(1, ChronoUnit.SECONDS))
										  .build();

		return componentRepository.save(newComponent);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Component updateFromRequest(ComponentRequest request) throws VempainEntityNotFoundException, VempainComponentException,
																		VempainAclException, VempainAbstractException {
		var userId = accessService.getValidUserId();
		Component component;

		try {
			component = findById(request.getId());
		} catch (VempainComponentException e) {
			log.error("Could not find component to be updated by request {}", request);
			throw new VempainEntityNotFoundException("Component can not be updated because it does not exist", COMPONENT_ID);
		}

		if (!accessService.hasModifyPermission(component.getAclId())) {
			log.error("User {} has no permission to modify component {}", userId, request.getId());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User has no permission to update component");
		}

		// We remove the old ACL entries and replace them with the new one from the request
		aclService.updateFromRequestList(request.getAcls());
		component.setCompData(request.getCompData());
		component.setCompName(request.getCompName());
		component.setModifier(userId);
		component.setModified(Instant.now());

		validateComponent(component);
		return componentRepository.save(component);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteByUser(Long componentId) throws VempainAclException {
		var userId = accessService.getValidUserId();

		var optionalComponent = componentRepository.findById(componentId);
		if (optionalComponent.isEmpty()) {
			log.error("Could not delete non-existing component with ID {}", componentId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}

		var component = optionalComponent.get();

		if (!accessService.hasDeletePermission(component.getAclId())) {
			log.error("User {} tried to delete component {} ({}) with insufficient permissions", userId, component.getId(),
					  component.getCompName());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.UNAUTHORIZED_ACCESS);
		}

		try {
			log.info("component ACL ID: {}", component.getAclId());
			aclService.deleteByAclId(component.getAclId());
		} catch (VempainEntityNotFoundException e) {
			log.warn("The layout referred to non-existing ACL ID: {}", component.getAclId());
		} catch (Exception e) {
			log.error("Failed to remove ACL ID {} for layout {}", component.getAclId(), componentId, e);
			throw new VempainAclException("Failed to remove ACL");
		}

		try {
			componentRepository.delete(component);
		} catch (Exception e) {
			log.error("Failed to remove component: {}", component, e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
		}
	}

	private void validateComponent(Component component) throws VempainComponentException, VempainAbstractException {
		if (component.getCompData() == null || component.getCompData()
														.isBlank()) {
			log.error("Component data is not set: {}", component);
			throw new VempainComponentException("Component data is not set");
		}

		if (component.getCompName() == null || component.getCompName()
														.isBlank()) {
			log.error("Component name is not set: {}", component);
			throw new VempainComponentException("Component name is not set");
		}

		aclService.validateAbstractData(component);
	}
}
