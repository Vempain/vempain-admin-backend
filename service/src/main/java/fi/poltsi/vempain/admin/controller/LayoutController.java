package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.LayoutRequest;
import fi.poltsi.vempain.admin.api.response.DeleteResponse;
import fi.poltsi.vempain.admin.api.response.LayoutResponse;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.exception.VempainLayoutException;
import fi.poltsi.vempain.admin.rest.LayoutAPI;
import fi.poltsi.vempain.admin.service.DeleteService;
import fi.poltsi.vempain.admin.service.LayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class LayoutController implements LayoutAPI {
	private final LayoutService layoutService;
	private final DeleteService deleteService;

	@Override
	public ResponseEntity<List<LayoutResponse>> getLayouts() {
		return ResponseEntity.ok(layoutService.findAllByUser());
	}

	@Override
	public ResponseEntity<LayoutResponse> getLayoutById(Long layoutId) {
		if ((layoutId == null) || layoutId < 1) {
			log.error(VempainMessages.MALFORMED_ID_IN_REQUEST_MSG, layoutId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_ID_IN_REQUEST);
		}

		try {
			return ResponseEntity.ok(layoutService.findLayoutResponseByIdByUser(layoutId));
		} catch (VempainEntityNotFoundException e) {
			log.error("Could not find layout with ID {}", layoutId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}
	}

	@Override
	public ResponseEntity<LayoutResponse> getLayoutByName(String layoutName) {
        if (layoutName == null) {
            log.error("The given layout name is null");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_NAME_IN_REQUEST);
        }

		layoutName = layoutName.trim();

		if (layoutName.isBlank()) {
			log.error("The given layout name is empty or malformed: " + layoutName);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_NAME_IN_REQUEST);
		}

		try {
			return ResponseEntity.ok(layoutService.findLayoutResponseByLayoutNameByUser(layoutName));
		} catch (VempainLayoutException e) {
			log.error("Failed to fetch layout with given layout name: " + layoutName);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}
	}

	@Override
	public ResponseEntity<LayoutResponse> saveLayout(LayoutRequest layoutRequest) {
		// Check that we have relevant data set
		if (validateLayoutRequest(layoutRequest)) {
		    log.debug("Saving layout request");
			var layout = layoutService.saveLayoutRequestByUser(layoutRequest);
			return ResponseEntity.ok(layoutService.createLayoutResponse(layout));
		} else {
			log.error("Malformed request to create a new layout: {}", layoutRequest);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
		}
	}

	@Override
	public ResponseEntity<LayoutResponse> updateLayout(LayoutRequest layoutRequest) {
        log.debug("Updating layout {}", layoutRequest);

        if ((layoutRequest == null) || layoutRequest.getId() < 1) {
            log.error(VempainMessages.MALFORMED_ID_IN_REQUEST_MSG, layoutRequest);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_ID_IN_REQUEST);
		}

		// Check that we have relevant data set
		if (validateLayoutRequest(layoutRequest)) {
            log.debug("Layout request validated: {}", layoutRequest);
			var layout = layoutService.updateByUser(layoutRequest);
			return ResponseEntity.ok(layoutService.createLayoutResponse(layout));
		} else {
			log.error("Received a malformed layoutRequest: {}", layoutRequest);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
		}
	}

	@Override
	public ResponseEntity<DeleteResponse> removeLayoutById(Long layoutId) {
        if ((layoutId == null) || layoutId < 1) {
            log.error(VempainMessages.MALFORMED_ID_IN_REQUEST_MSG, layoutId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_ID_IN_REQUEST);
		}

		try {
			deleteService.deleteLayoutById(layoutId);

			DeleteResponse deleteResponse = DeleteResponse.builder()
														  .count(1L)
														  .name("layout")
														  .id(layoutId)
														  .build();

			return ResponseEntity.ok(deleteResponse);
		} catch (VempainEntityNotFoundException e) {
			log.error("User tried to delete a non-existing layout: {}", layoutId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}
	}

	private boolean validateLayoutRequest(LayoutRequest layoutRequest) {
	    log.debug("Validating layout request: {}", layoutRequest);
		return layoutRequest != null && layoutRequest.getLayoutName() != null &&
			   !layoutRequest.getLayoutName().isBlank() &&
			   layoutRequest.getAcls() != null && !layoutRequest.getAcls().isEmpty();
	}
}
