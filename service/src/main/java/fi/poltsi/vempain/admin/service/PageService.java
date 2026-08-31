package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.PagePagedRequest;
import fi.poltsi.vempain.admin.api.request.PageRequest;
import fi.poltsi.vempain.admin.api.response.PageResponse;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.repository.PageRepository;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
@Service
public class PageService {
	private final PageRepository pageRepository;
	private final AclService     aclService;
	private final AccessService  accessService;

	public Iterable<Page> findAll() {
		return pageRepository.findAll();
	}

	public List<Page> findAllByFormId(long formId) {
		return pageRepository.findByFormId(formId);
	}

	public List<Page> findAllByUser() {
		Iterable<Page> pages = findAll();
		ArrayList<Page> accessiblePages = new ArrayList<>();

		for (Page page : pages) {
			if (accessService.hasReadPermission(page.getAclId())) {
				accessiblePages.add(page);
			}

		}

		return accessiblePages;
	}

	public PagedResponse<PageResponse> findPagedByUser(PagePagedRequest request) {
		var pages = new ArrayList<>(findAllByUser());
		var search = request.getSearch();
		if (search != null && !search.isBlank()) {
			var query = request.getCaseSensitive() != null && request.getCaseSensitive()
			            ? search
			            : search.toLowerCase(Locale.ROOT);
			pages.removeIf(page -> !contains(page.getPagePath(), query, request)
			                       && !contains(page.getTitle(), query, request)
			                       && !contains(page.getHeader(), query, request));
		}

		var sortBy = request.getSortBy();
		if (sortBy != null && !sortBy.isBlank()) {
			var comparator = pageComparator(sortBy);
			if (request.getDirection() == org.springframework.data.domain.Sort.Direction.DESC) {
				comparator = comparator.reversed();
			}
			pages.sort(comparator);
		}

		var page = request.getPage();
		var size = request.getSize();
		var totalElements = pages.size();
		var totalPages = (int) Math.ceil((double) totalElements / size);
		var fromIndex = Math.min(page * size, totalElements);
		var toIndex = Math.min(fromIndex + size, totalElements);
		var content = pages.subList(fromIndex, toIndex)
		                   .stream()
		                   .map(this::toUnpopulatedResponse)
		                   .toList();
		return PagedResponse.of(content, page, size, totalElements, totalPages, page == 0, page + 1 >= totalPages);
	}

	private boolean contains(String value, String query, PagePagedRequest request) {
		if (value == null) {
			return false;
		}
		var candidate = request.getCaseSensitive() != null && request.getCaseSensitive()
		                ? value
		                : value.toLowerCase(Locale.ROOT);
		return candidate.contains(query);
	}

	private Comparator<Page> pageComparator(String sortBy) {
		return switch (sortBy) {
			case "id" -> Comparator.comparing(Page::getId);
			case "parent_id" -> Comparator.comparing(page -> page.getParentId() == null ? 0L : page.getParentId());
			case "form_id" -> Comparator.comparing(Page::getFormId);
			case "page_path" -> Comparator.comparing(Page::getPagePath, String.CASE_INSENSITIVE_ORDER);
			case "title" -> Comparator.comparing(Page::getTitle, String.CASE_INSENSITIVE_ORDER);
			case "created" -> Comparator.comparing(Page::getCreated);
			case "modified" -> Comparator.comparing(Page::getModified,
													Comparator.nullsFirst(Comparator.naturalOrder()));
			default -> Comparator.comparing(Page::getId);
		};
	}

	private PageResponse toUnpopulatedResponse(Page page) {
		return page.toResponse();
	}

	public Page findById(long pageId) {
		return pageRepository.findById(pageId);
	}

	public Page findByPath(String path) throws VempainEntityNotFoundException {
		var page = pageRepository.findByPagePath(path);

		if (page == null) {
			log.error("Could not find a page with path: {}", path);
			throw new VempainEntityNotFoundException("Failed to find page by path", "page");
		}
		return page;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Page save(Page page) {
		return pageRepository.save(page);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Page saveFromPageRequest(PageRequest request) {
		log.debug("Received call to save page request: {}", request);

		var userId = accessService.getValidUserId();

		try {
			log.debug("Checking if path of the new page already exists: {}", request.getPagePath());
			var otherPage = findByPath(request.getPagePath());
			log.error("Page already exists with path {}. ID {}", request.getPagePath(), otherPage.getId());
			throw new ResponseStatusException(HttpStatus.CONFLICT, VempainMessages.OBJECT_NAME_ALREADY_EXISTS);
		} catch (VempainEntityNotFoundException e) {
			log.info("No page with path {} found, can save it", request.getPagePath());
		}

		long aclId = aclService.saveNewAclForObject(request.getAcls());

		var page = Page.builder()
		               .aclId(aclId)
		               .formId(request.getFormId())
		               .header(request.getHeader()
		                              .trim())
		               .title(request.getTitle()
		                             .trim())
		               .pagePath(request.getPagePath())
		               .body(request.getBody()
		                            .trim())
		               .indexList(request.isIndexList())
		               .locked(false)
		               .secure(request.isSecure())
		               .creator(userId)
		               .created(Instant.now())
		               .modifier(null)
		               .modified(null)
		               .build();

		return save(page);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void deleteById(long pageId) throws ProcessingFailedException, VempainEntityNotFoundException {
		var userId = accessService.getValidUserId();
		var page = pageRepository.findById(pageId);

		if (page == null) {
			log.error("Tried to delete a non-existing page with ID: {}", pageId);
			throw new VempainEntityNotFoundException("Fail to delete a page with non-existing ID", "page");
		}

		if (!accessService.hasDeletePermission(page.getAclId())) {
			log.error("User {} tried to delete page {} without delete permission", userId, pageId);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access tried to delete page");
		}
		// Delete also the Acl
		try {
			log.debug("Layout ACL ID: {}", page.getAclId());
			aclService.deleteByAclId(page.getAclId());
		} catch (Exception e) {
			log.error("Failed to remove acl: {}", page.getAclId(), e);
			throw new ProcessingFailedException("Failed to delete ACL");
		}

		try {
			log.debug("Layout ID: {}", pageId);
			pageRepository.delete(page);
		} catch (Exception e) {
			log.error("Failed to remove page: {}", page, e);
			throw new ProcessingFailedException("Failed to delete page");
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Page updateFromRequest(PageRequest request) {
		var userId = accessService.getValidUserId();

		var page = findById(request.getId());
		if (page == null) {
			log.error("User {} attempted to update non-existing layout: {}", userId, request);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}

		if (!accessService.hasModifyPermission(page.getAclId())) {
			log.error("User {} has no permission to modify page {}", userId, request.getId());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.UNAUTHORIZED_ACCESS);
		}

		// If the path is updated, then make sure it is not already used by some other page
		if (!request.getPagePath()
		            .trim()
		            .equals(page.getPagePath()
		                        .trim())) {
			log.debug("User is updating the path of page ID {} from {} to {}", request.getId(), page.getPagePath(), request.getPagePath());

			try {
				var pathPage = findByPath(request.getPagePath()
				                                 .trim());

				if (!pathPage.getId()
				             .equals(page.getId())) {
					log.error("Failed to update page as the path {} already exists and belongs to page ID {}", request.getPagePath()
					                                                                                                  .trim(), pathPage.getId());
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
				}
			} catch (VempainEntityNotFoundException e) {
				log.info("Page path can be updated from {} to {}", page.getPagePath(), request.getPagePath());
			}
		}

		try {
			aclService.updateFromRequestList(request.getAcls());
		} catch (VempainAclException e) {
			log.error("Failed to update ACLs from request for page {}: {}", page.getId(), request.getAcls());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
		}

		var parentId = (request.getParentId() != null && request.getParentId() > 0L) ? request.getParentId() : null;

		try {
			page.setBody(request.getBody()
			                    .trim());
			page.setPagePath(request.getPagePath()
			                        .trim());
			page.setTitle(request.getTitle()
			                     .trim());
			page.setHeader(request.getHeader()
			                      .trim());
			page.setIndexList(request.isIndexList());
			page.setFormId(request.getFormId());
			page.setParentId(parentId);
			page.setModifier(userId);
			page.setModified(Instant.now());

			return save(page);
		} catch (Exception e) {
			log.error("Failed to update page to database: {}", request);
			log.error("Exception message: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
		}
	}

	public void deleteByUser(long pageId) {
		var userId = accessService.getValidUserId();

		var page = findById(pageId);

		if (page == null) {
			log.error("Could not delete non-existing page with ID {}", pageId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}

		if (!accessService.hasDeletePermission(page.getAclId())) {
			log.error("User {} tried to delete page {} ({}) with insufficient permissions", userId, page.getId(),
			          page.getPagePath());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.UNAUTHORIZED_ACCESS);
		}

		try {
			log.debug("component ACL ID: {}", page.getAclId());
			aclService.deleteByAclId(page.getAclId());
		} catch (VempainEntityNotFoundException e) {
			log.warn("The layout referred to non-existing ACL ID: {}", page.getAclId());
		} catch (Exception e) {
			log.error("Failed to remove ACL ID {} for layout {}", page.getAclId(), page, e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
		}

		try {
			pageRepository.delete(page);
		} catch (Exception e) {
			log.error("Failed to remove page: {}", page, e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
		}
	}

	public PageResponse populateResponse(Page page) {
		var response = page.toResponse();
		response.setAcls(aclService.getAclResponses(page.getAclId()));
		return response;
	}
}
