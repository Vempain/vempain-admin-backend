package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.LayoutRequest;
import fi.poltsi.vempain.admin.api.response.LayoutResponse;
import fi.poltsi.vempain.admin.entity.Layout;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.exception.VempainLayoutException;
import fi.poltsi.vempain.admin.repository.LayoutRepository;
import fi.poltsi.vempain.auth.exception.VempainAbstractException;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.service.AclService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class LayoutService {
	private final LayoutRepository layoutRepository;
	private final AclService       aclService;
	private final AccessService    accessService;

	public Iterable<Layout> findAll() {
		return layoutRepository.findAll();
	}

	public List<LayoutResponse> findAllByUser() {
		Iterable<Layout> layouts = findAll();
		ArrayList<LayoutResponse> responses = new ArrayList<>();

		for (Layout layout : layouts) {
			if (accessService.hasReadPermission(layout.getAclId())) {
				var layoutResponse = layout.getLayoutResponse();
				layoutResponse.setAcls(aclService.getAclResponses(layout.getAclId()));
				responses.add(layoutResponse);
			}
		}

		return responses;
	}

	public Layout findById(long layoutId) throws VempainEntityNotFoundException {
		var optionalLayout = layoutRepository.findById(layoutId);

		if (optionalLayout.isEmpty()) {
			log.error("Failed to find a layout with ID {}", layoutId);
			throw new VempainEntityNotFoundException(VempainMessages.NO_LAYOUT_FOUND_BY_ID, "layout");
		}
		return optionalLayout.get();
	}

	public Layout findByIdByUser(long layoutId) throws VempainEntityNotFoundException {
		var userId = accessService.getValidUserId();

		var layout = findById(layoutId);

		if (accessService.hasReadPermission(layout.getAclId())) {
			return layout;
		} else {
			log.error("User {} tried to access layout {} ({}) with insufficient permissions", userId, layoutId, layout.getLayoutName());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.UNAUTHORIZED_ACCESS);
		}
	}

	public LayoutResponse findLayoutResponseByIdByUser(long layoutId) throws VempainEntityNotFoundException {
		var layout = findByIdByUser(layoutId);
		return createLayoutResponse(layout);
	}

	public LayoutResponse createLayoutResponse(Layout layout) {
		var response = layout.getLayoutResponse();
		response.setAcls(aclService.getAclResponses(layout.getAclId()));
		return response;
	}

	public Layout findByLayoutName(String layoutName) throws VempainLayoutException {
		var optionalLayout = layoutRepository.findByLayoutName(layoutName);

		if (optionalLayout.isEmpty()) {
			log.error("Failed to find layout by the name '{}'", layoutName);
			throw new VempainLayoutException(VempainMessages.OBJECT_NOT_FOUND);
		}
		return optionalLayout.get();
	}

	public LayoutResponse findLayoutResponseByLayoutNameByUser(String layoutName) throws VempainLayoutException {
		var userId = accessService.getValidUserId();

		var layout = findByLayoutName(layoutName);

		if (accessService.hasReadPermission(layout.getAclId())) {
			return createLayoutResponse(layout);
		} else {
			log.error("User {} tried to access layout {} ({}) with insufficient permissions", userId, layout.getId(), layout.getLayoutName());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.UNAUTHORIZED_ACCESS);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Layout save(Layout layout) throws VempainLayoutException, VempainAbstractException {
		validateLayout(layout);
		return layoutRepository.save(layout);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Layout saveLayoutRequestByUser(LayoutRequest layoutRequest) {
		var userId = accessService.getValidUserId();

		try {
			// Check if the layout name already exists
			var otherLayout = findByLayoutName(layoutRequest.getLayoutName()
															.trim());
			log.error("Could not save a new layout when the layout name {} already exists in the database ({})",
					  layoutRequest.getLayoutName(), otherLayout.getId());
			throw new ResponseStatusException(HttpStatus.CONFLICT, VempainMessages.OBJECT_NAME_ALREADY_EXISTS);
		} catch (VempainLayoutException vempainLayoutException) {
			try {
				// First we save the ACLs
				long nextAclId = aclService.getNextAclId();
				aclService.saveAclRequests(nextAclId, layoutRequest.getAcls());

				var layout = Layout.builder()
								   .aclId(nextAclId)
								   .layoutName(layoutRequest.getLayoutName()
															.trim())
								   .structure(layoutRequest.getStructure()
														   .trim())
								   .locked(false)
								   .creator(userId)
								   .created(Instant.now())
								   .modifier(userId)
								   .modified(Instant.now())
								   .build();
				var newLayout = save(layout);
				log.info("Layout ID: {}", newLayout.getId());
				log.info("Adding layout:\n{}", newLayout);
				return newLayout;
			} catch (VempainAclException e) {
				log.error("Failed to store ACL list for new layout: ", e);
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
			} catch (VempainLayoutException | VempainAbstractException e) {
				log.error("Failed to store new layout: ", e);
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Layout updateByUser(LayoutRequest layoutRequest) {
		var userId = accessService.getValidUserId();

		// Check if the layout already exists
		Layout layout;

		try {
			layout = findById(layoutRequest.getId());
		} catch (VempainEntityNotFoundException e) {
			log.error("User attempted to update non-existing layout: {}", layoutRequest);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}

		if (!accessService.hasModifyPermission(layout.getAclId())) {
			log.error("User {} tried to update layout {} ({}) with insufficient permissions", userId, layout.getId(),
					  layout.getLayoutName());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.UNAUTHORIZED_ACCESS);
		}

		try {
			// Save the ACLs first
			aclService.updateFromRequestList(layoutRequest.getAcls());

			// The only fields that user can update is the name and structure, so we update these
			layout.setLayoutName(layoutRequest.getLayoutName());
			layout.setStructure(layoutRequest.getStructure());
			// Also update the modified/r fields
			layout.setModifier(userId);
			layout.setModified(Instant.now());
			return save(layout);
		} catch (Exception e) {
			log.error("Failed to update layout to database: {}", layoutRequest);
			log.error("Exception message: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void delete(long layoutId) throws ProcessingFailedException, VempainAclException, VempainEntityNotFoundException {
		var optionalLayout = layoutRepository.findById(layoutId);

		if (optionalLayout.isEmpty()) {
			log.error("No layout with ID {} found for deletion", layoutId);
			throw new VempainEntityNotFoundException("No layout can be found for deletion", "layout");
		}

		var layout = optionalLayout.get();
		// Delete also the Acl
		try {
			log.info("Layout ACL ID: {}", layout.getAclId());
			aclService.deleteByAclId(layout.getAclId());
		} catch (VempainEntityNotFoundException e) {
			log.warn("The layout referred to non-existing ACL ID: {}", layout.getAclId());
		} catch (Exception e) {
			log.error("Failed to remove ACL ID {} for layout {}", layout.getAclId(), layoutId, e);
			throw new VempainAclException("Failed to remove ACL");
		}

		try {
			log.info("Layout ID: {}", layoutId);
			layoutRepository.delete(layout);
		} catch (Exception e) {
			log.error("Failed to delete layout: {}", layout, e);
			throw new ProcessingFailedException("Failed to delete layout");
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteByUser(long layoutId) {
		var userId = accessService.getValidUserId();

		Layout layout;

		try {
			layout = findById(layoutId);
		} catch (VempainEntityNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}

		if (accessService.hasDeletePermission(layout.getAclId())) {
			try {
				delete(layoutId);
			} catch (Exception e) {
				log.error("Attempting to delete layout {} failed", layoutId, e);
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
			}
		} else {
			log.error("User {} tried to delete layout {} ({}) with insufficient permissions", userId, layout.getId(),
					  layout.getLayoutName());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.UNAUTHORIZED_ACCESS);
		}
	}

	private void validateLayout(Layout layout) throws VempainLayoutException, VempainAbstractException {
		if (layout == null) {
			throw new VempainLayoutException("Layout is null");
		}

		if (layout.getLayoutName() == null ||
			layout.getLayoutName()
				  .isBlank()) {
			throw new VempainLayoutException("Layout name is null or blank");
		}

		if (layout.getStructure() == null ||
			layout.getStructure()
				  .isBlank()) {
			throw new VempainLayoutException("Layout structure is null or blank");
		}

		aclService.validateAbstractData(layout);
	}
}
