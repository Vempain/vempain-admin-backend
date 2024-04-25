package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.request.PageRequest;
import fi.poltsi.vempain.admin.api.response.PageResponse;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.exception.ProcessingFailedException;
import fi.poltsi.vempain.admin.exception.VempainAclException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.repository.PageRepository;
import fi.poltsi.vempain.site.repository.SitePageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PageService extends AbstractService {
	private final PageRepository pageRepository;
	private final SitePageRepository sitePageRepository;

	@Autowired
	public PageService(AclService aclService, AccessService accessService, PageRepository pageRepository, SitePageRepository sitePageRepository) {
		super(aclService, accessService);
		this.pageRepository = pageRepository;
		this.sitePageRepository = sitePageRepository;
	}

	public Iterable<Page> findAll() {
		return pageRepository.findAll();
	}

	public List<Page> findAllByFormId(long formId) {
		return pageRepository.findByFormId(formId);
	}

	public List<Page> findAllByUser() {
		Iterable<Page>  pages           = findAll();
		ArrayList<Page> accessiblePages = new ArrayList<>();

		for (Page page : pages) {
			if (accessService.hasReadPermission(page.getAclId())) {
				accessiblePages.add(page);
			}
		}

		return accessiblePages;
	}

	public Page findById(long pageId) {
		return pageRepository.findById(pageId);
	}

	public Page findByPath(String path) throws VempainEntityNotFoundException {
		var page = pageRepository.findByPath(path);

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

		var userId = getUserId();

		try {
			log.debug("Checking if path of the new page already exists: {}", request.getPath());
			var otherPage = findByPath(request.getPath());
			log.error("Page already exists with path {}. ID {}", request.getPath(), otherPage.getId());
			throw new ResponseStatusException(HttpStatus.CONFLICT, VempainMessages.OBJECT_NAME_ALREADY_EXISTS);
		} catch (VempainEntityNotFoundException e) {
			log.info("No page with path {} found, can save it", request.getPath());
		}

		long aclId = aclService.saveNewAclForObject(request.getAcls());

		var page = Page.builder()
					   .aclId(aclId)
					   .formId(request.getFormId())
					   .header(request.getHeader().trim())
					   .title(request.getTitle().trim())
					   .path(request.getPath())
					   .body(request.getBody().trim())
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
		var userId = getUserId();
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
			log.info("Layout ACL ID: {}", page.getAclId());
			aclService.deleteByAclId(page.getAclId());
		} catch (Exception e) {
			log.error("Failed to remove acl: {}", page.getAclId(), e);
			throw new ProcessingFailedException("Failed to delete ACL");
		}

		try {
			log.info("Layout ID: {}", pageId);
			pageRepository.delete(page);
		} catch (Exception e) {
			log.error("Failed to remove page: {}", page, e);
			throw new ProcessingFailedException("Failed to delete page");
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Page updateFromRequest(PageRequest request) {
		var userId = getUserId();

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
		if (!request.getPath().trim().equals(page.getPath().trim())) {
			log.info("User is updating the path of page ID {} from {} to {}", request.getId(), page.getPath(), request.getPath());

			try {
				var pathPage = findByPath(request.getPath().trim());

				if (pathPage.getId() != page.getId()) {
					log.error("Failed to update page as the path {} already exists", request.getPath().trim());
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, VempainMessages.MALFORMED_OBJECT_IN_REQUEST);
				}
			} catch (VempainEntityNotFoundException e) {
				log.info("Page path can be updated from {} to {}", page.getPath(), request.getPath());
			}
		}

		try {
			aclService.updateFromRequestList(request.getAcls());
		} catch (VempainAclException e) {
			log.error("Failed to update ACLs from request for page {}: {}", page.getId(), request.getAcls());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, VempainMessages.INTERNAL_ERROR);
		}

		try {
			page.setBody(request.getBody().trim());
			page.setPath(request.getPath().trim());
			page.setTitle(request.getTitle().trim());
			page.setHeader(request.getHeader().trim());
			page.setIndexList(request.isIndexList());
			page.setFormId(request.getFormId());
			page.setParentId(request.getParentId());
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
		var userId = getUserId();

		var page = findById(pageId);

		if (page == null) {
			log.error("Could not delete non-existing page with ID {}", pageId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, VempainMessages.OBJECT_NOT_FOUND);
		}

		if (!accessService.hasDeletePermission(page.getAclId())) {
			log.error("User {} tried to delete page {} ({}) with insufficient permissions", userId, page.getId(),
					  page.getPath());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.UNAUTHORIZED_ACCESS);
		}

		try {
			log.info("component ACL ID: {}", page.getAclId());
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
		var publishedPage = sitePageRepository.findById(page.getId());
		publishedPage.ifPresent(sitePage -> response.setPublished(sitePage.getPublished()));

		response.setAcls(aclService.getAclResponses(page.getAclId()));
		return response;

	}
}
