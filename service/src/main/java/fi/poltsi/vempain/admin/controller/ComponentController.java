package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.ComponentRequest;
import fi.poltsi.vempain.admin.api.response.ComponentResponse;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
import fi.poltsi.vempain.admin.entity.Component;
import fi.poltsi.vempain.admin.exception.VempainAbstractException;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.rest.ComponentAPI;
import fi.poltsi.vempain.admin.service.AclService;
import fi.poltsi.vempain.admin.service.ComponentService;
import fi.poltsi.vempain.admin.service.DeleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class ComponentController implements ComponentAPI {
	private final ComponentService componentService;
	private final AclService       aclService;
	private final DeleteService    deleteService;

	@Override
	public ResponseEntity<List<ComponentResponse>> getComponents() {
		List<Component> components = componentService.findAllByUser();

		ArrayList<ComponentResponse> responses = new ArrayList<>();

		for (Component component : components) {
			responses.add(createResponseWithAcls(component));
		}

		return ResponseEntity.ok(responses);
	}

	@Override
	public ResponseEntity<ComponentResponse> getComponentById(Long componentId) {
		if (componentId == null || componentId < 1L) {
			log.error(VempainMessages.MALFORMED_ID_IN_REQUEST_MSG, componentId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_ID_IN_REQUEST);
		}

		var component = componentService.findByIdByUser(componentId);
		return ResponseEntity.ok(createResponseWithAcls(component));
	}

	@Override
	public ResponseEntity<ComponentResponse> addComponent(ComponentRequest componentRequest) {
		verifyComponentRequest(componentRequest);

		var component = componentService.saveFromRequest(componentRequest);
		return ResponseEntity.ok(createResponseWithAcls(component));
	}

	@Override
	public ResponseEntity<ComponentResponse> updateComponent(ComponentRequest componentRequest) {
		if (componentRequest == null || componentRequest.getId() < 1L) {
			log.error(VempainMessages.MALFORMED_ID_IN_REQUEST_MSG, componentRequest);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_ID_IN_REQUEST);
		}

		verifyComponentRequest(componentRequest);

		try {
			var component = componentService.updateFromRequest(componentRequest);
			return ResponseEntity.ok(createResponseWithAcls(component));
		} catch (VempainComponentException | VempainAclException | VempainAbstractException e) {
			log.error("Failed to update component:", e);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component data failed validation");
		} catch (VempainEntityNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}
	}

	@Override
	public ResponseEntity<DeleteResponse> deleteComponentById(long componentId) {
		if (componentId < 1L) {
			log.error(VempainMessages.MALFORMED_ID_IN_REQUEST_MSG, componentId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_ID_IN_REQUEST);
		}

		try {
			deleteService.deleteComponentById(componentId);
			return ResponseEntity.ok(DeleteResponse.builder()
												   .count(1)
												   .id(componentId)
												   .name("Component")
												   .timestamp(Instant.now())
												   .httpStatus(HttpStatus.OK)
												   .build());
		} catch (VempainEntityNotFoundException e) {
			log.error("Failed to delete a component:\n{}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
		}
	}

	private void verifyComponentRequest(ComponentRequest componentRequest) {
		if (componentRequest == null ||
			componentRequest.getCompName() == null ||
			componentRequest.getCompName().isBlank() ||
			componentRequest.getCompData() == null ||
			componentRequest.getCompData().isBlank() ||
			componentRequest.getAcls() == null ||
			componentRequest.getAcls().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
		}
	}

	private ComponentResponse createResponseWithAcls(Component component) {
		var response = component.getComponentResponse();
		response.setAcls(aclService.getAclResponses(component.getAclId()));
		return response;
	}
}
