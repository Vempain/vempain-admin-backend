package fi.poltsi.vempain.site.service;

import fi.poltsi.vempain.admin.api.site.request.WebSiteAclRequest;
import fi.poltsi.vempain.admin.api.site.response.WebSiteAclResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteAclUsersResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteResourceResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteUserResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.site.entity.WebSiteAcl;
import fi.poltsi.vempain.site.repository.WebSiteAclRepository;
import fi.poltsi.vempain.site.repository.WebSiteFileRepository;
import fi.poltsi.vempain.site.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.site.repository.WebSitePageRepository;
import fi.poltsi.vempain.site.repository.WebSiteUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum.GALLERY;
import static fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum.PAGE;
import static fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum.SITE_FILE;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSiteAclService {
	private final WebSiteAclRepository  webSiteAclRepository;
	private final WebSiteUserRepository webSiteUserRepository;
	private final WebSiteFileRepository webSiteFileRepository;
	private final WebSiteGalleryRepository webSiteGalleryRepository;
	private final AccessService         accessService;
	private final WebSitePageRepository webSitePageRepository;

	/**
	 * Find all ACL entries
	 *
	 * @return List of ACL responses
	 */
	public List<WebSiteAclResponse> findAll() {
		var responses = new ArrayList<WebSiteAclResponse>();
		webSiteAclRepository.findAll()
							.forEach(acl -> responses.add(acl.toResponse()));
		return responses;
	}

	/**
	 * Find an ACL entry by ID
	 *
	 * @param id The ACL entry ID
	 * @return ACL response
	 */
	public WebSiteAclResponse findById(Long id) {
		return webSiteAclRepository.findById(id)
								   .map(WebSiteAcl::toResponse)
								   .orElseThrow(() -> {
									   log.error("Site ACL entry not found with ID: {}", id);
									   return new ResponseStatusException(HttpStatus.NOT_FOUND, "Site ACL entry not found");
								   });
	}

	/**
	 * Find all users assigned to a specific ACL ID
	 *
	 * @param aclId The ACL ID
	 * @return Response containing ACL ID and list of users
	 */
	public WebSiteAclUsersResponse findUsersByAclId(Long aclId) {
		var aclEntries = webSiteAclRepository.findByAclId(aclId);

		var users = new ArrayList<WebSiteAclUsersResponse.UserSummary>();

		for (var acl : aclEntries) {
			webSiteUserRepository.findById(acl.getUserId())
								 .ifPresent(user ->
													users.add(WebSiteAclUsersResponse.UserSummary.builder()
																								 .userId(user.getId())
																								 .username(user.getUsername())
																								 .build())
								 );
		}

		return WebSiteAclUsersResponse.builder()
									  .aclId(aclId)
									  .users(users)
									  .build();
	}

	/**
	 * Find all resources accessible to a specific user
	 *
	 * @param userId The user ID
	 * @return Response containing user info and accessible resources
	 */
	public WebSiteUserResponse findResourcesByUserId(Long userId) {
		var user = webSiteUserRepository.findById(userId)
										.orElseThrow(() -> {
											log.error("Site web user not found with ID: {}", userId);
											return new ResponseStatusException(HttpStatus.NOT_FOUND, "Site web user not found");
										});

		var aclIds = webSiteAclRepository.findAclIdsByUserId(userId);
		var resources = new ArrayList<WebSiteResourceResponse>();

		// Find files with matching ACL IDs
		for (var file : webSiteFileRepository.findAll()) {
			if (aclIds.contains(file.getAclId())) {
				resources.add(WebSiteResourceResponse.builder()
													 .resourceType(SITE_FILE)
													 .resourceId(file.getId())
													 .name(file.getPath())
													 .path(file.getPath())
													 .aclId(file.getAclId())
													 .fileType(file.getFileType()
																   .name())
													 .build());
			}
		}

		// Find galleries with matching ACL IDs
		for (var gallery : webSiteGalleryRepository.findAll()) {
			if (aclIds.contains(gallery.getAclId())) {
				resources.add(WebSiteResourceResponse.builder()
													 .resourceType(GALLERY)
													 .resourceId(gallery.getId())
													 .name(gallery.getShortname())
													 .path(gallery.getShortname())
													 .aclId(gallery.getAclId())
													 .fileType("Gallery")
													 .build());
			}
		}
		// Find pages with matching ACL IDs
		for (var page : webSitePageRepository.findAll()) {
			if (aclIds.contains(page.getAclId())) {
				resources.add(WebSiteResourceResponse.builder()
													 .resourceType(PAGE)
													 .resourceId(page.getId())
													 .name(page.getTitle())
													 .path(page.getPath())
													 .aclId(page.getAclId())
													 .fileType("Page")
													 .build());
			}
		}

		return user.toResponse(resources);
	}

	/**
	 * Create a new ACL entry linking a user to an ACL ID
	 *
	 * @param request The ACL request
	 * @return Created ACL response
	 */
	@Transactional
	public WebSiteAclResponse create(WebSiteAclRequest request) {
		var adminUserId = accessService.getValidUserId();

		// Verify user exists
		if (!webSiteUserRepository.existsById(request.getUserId())) {
			log.error("Site web user not found with ID: {}", request.getUserId());
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Site web user not found");
		}

		// Check if this ACL assignment already exists
		var existing = webSiteAclRepository.findByAclIdAndUserId(request.getAclId(), request.getUserId());
		if (existing != null) {
			log.error("ACL assignment already exists for ACL ID {} and user ID {}", request.getAclId(), request.getUserId());
			throw new ResponseStatusException(HttpStatus.CONFLICT, "ACL assignment already exists");
		}

		var acl = WebSiteAcl.builder()
							.aclId(request.getAclId())
							.userId(request.getUserId())
							.creator(adminUserId)
							.created(Instant.now())
							.build();

		var saved = webSiteAclRepository.save(acl);
		log.debug("Created new site ACL entry ID: {} linking ACL ID {} to user ID {} by admin user: {}",
				  saved.getId(), request.getAclId(), request.getUserId(), adminUserId);
		return saved.toResponse();
	}

	/**
	 * Delete an ACL entry
	 *
	 * @param id The ACL entry ID to delete
	 */
	@Transactional
	public void delete(Long id) {
		var adminUserId = accessService.getValidUserId();

		if (!webSiteAclRepository.existsById(id)) {
			log.error("Site ACL entry not found with ID: {}", id);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Site ACL entry not found");
		}

		webSiteAclRepository.deleteById(id);
		log.debug("Deleted site ACL entry ID: {} by admin user: {}", id, adminUserId);
	}

	/**
	 * Delete all ACL entries for a specific ACL ID
	 *
	 * @param aclId The ACL ID
	 */
	@Transactional
	public void deleteByAclId(Long aclId) {
		var adminUserId = accessService.getValidUserId();
		webSiteAclRepository.deleteByAclId(aclId);
		log.info("Deleted all site ACL entries for ACL ID: {} by admin user: {}", aclId, adminUserId);
	}
}

