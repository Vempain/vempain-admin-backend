package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum;
import fi.poltsi.vempain.admin.api.site.request.WebSiteAclRequest;
import fi.poltsi.vempain.admin.api.site.request.WebSiteConfigurationRequest;
import fi.poltsi.vempain.admin.api.site.request.WebSiteUserRequest;
import fi.poltsi.vempain.admin.api.site.response.WebSiteAclResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteAclUsersResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteConfigurationResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteResourcePageResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteUserResponse;
import fi.poltsi.vempain.admin.rest.WebSiteManagementAPI;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import fi.poltsi.vempain.site.service.WebSiteAclService;
import fi.poltsi.vempain.site.service.WebSiteConfigurationService;
import fi.poltsi.vempain.site.service.WebSiteResourceService;
import fi.poltsi.vempain.site.service.WebSiteUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class WebSiteManagementController implements WebSiteManagementAPI {
	private final WebSiteUserService webSiteUserService;
	private final WebSiteAclService  webSiteAclService;
	private final AccessService      accessService;
	private final WebSiteResourceService webSiteResourceService;
	private final WebSiteConfigurationService webSiteConfigurationService;

	// ========== Site Web User Endpoints ==========

	@Override
	public ResponseEntity<List<WebSiteUserResponse>> getAllUsers() {
		accessService.checkAuthentication();
		log.debug("Fetching all site web users");
		return ResponseEntity.ok(webSiteUserService.findAll());
	}

	@Override
	public ResponseEntity<WebSiteUserResponse> getUserById(Long userId) {
		accessService.checkAuthentication();
		log.debug("Fetching site web user with ID: {}", userId);
		return ResponseEntity.ok(webSiteUserService.findById(userId));
	}

	@Override
	public ResponseEntity<WebSiteUserResponse> createUser(WebSiteUserRequest request) {
		accessService.checkAuthentication();
		log.debug("Creating new site web user with username: {}", request.getUsername());
		return ResponseEntity.ok(webSiteUserService.create(request));
	}

	@Override
	public ResponseEntity<WebSiteUserResponse> updateUser(Long userId, WebSiteUserRequest request) {
		accessService.checkAuthentication();
		log.debug("Updating site web user ID: {}", userId);
		return ResponseEntity.ok(webSiteUserService.update(userId, request));
	}

	@Override
	public ResponseEntity<Void> deleteUser(Long userId) {
		accessService.checkAuthentication();
		log.debug("Deleting site web user ID: {}", userId);
		webSiteUserService.delete(userId);
		return ResponseEntity.noContent()
							 .build();
	}

	// ========== Site ACL Endpoints ==========

	@Override
	public ResponseEntity<List<WebSiteAclResponse>> getAllAcls() {
		accessService.checkAuthentication();
		log.debug("Fetching all site ACL entries");
		return ResponseEntity.ok(webSiteAclService.findAll());
	}

	@Override
	public ResponseEntity<WebSiteAclUsersResponse> getUsersByAclId(Long aclId) {
		accessService.checkAuthentication();
		log.debug("Fetching users for ACL ID: {}", aclId);
		return ResponseEntity.ok(webSiteAclService.findUsersByAclId(aclId));
	}

	@Override
	public ResponseEntity<WebSiteUserResponse> getResourcesByUserId(Long userId) {
		accessService.checkAuthentication();
		log.debug("Fetching resources for user ID: {}", userId);
		return ResponseEntity.ok(webSiteAclService.findResourcesByUserId(userId));
	}

	@Override
	public ResponseEntity<WebSiteAclResponse> createAcl(WebSiteAclRequest request) {
		accessService.checkAuthentication();
		log.debug("Creating site ACL entry: ACL ID {} for user ID {}", request.getAclId(), request.getUserId());
		return ResponseEntity.ok(webSiteAclService.create(request));
	}

	@Override
	public ResponseEntity<Void> deleteAcl(Long id) {
		accessService.checkAuthentication();
		log.debug("Deleting site ACL entry ID: {}", id);
		webSiteAclService.delete(id);
		return ResponseEntity.noContent()
							 .build();
	}

	// ========== Site Resource Endpoints ==========

	@Override
	public ResponseEntity<WebSiteResourcePageResponse> getResources(WebSiteResourceEnum resourceType, FileTypeEnum fileType, String query, Long aclId,
																	String sort, String direction, int page, int size) {
		accessService.checkAuthentication();
		return ResponseEntity.ok(webSiteResourceService.listResources(resourceType, fileType, query, aclId, sort, direction, page, size));
	}

	// ========== Site configuration Endpoints ==========

	@Override
	public ResponseEntity<List<WebSiteConfigurationResponse>> getAllSiteConfigurations() {
		var configurations = webSiteConfigurationService.getAllConfigurations();
		return ResponseEntity.ok(configurations);
	}

	@Override
	public ResponseEntity<WebSiteConfigurationResponse> getSiteConfigurationById(Long id) {
		var configuration = webSiteConfigurationService.getConfigurationById(id);
		if (configuration != null) {
			return ResponseEntity.ok(configuration);
		}

		return ResponseEntity.notFound()
							 .build();
	}

	@Override
	public ResponseEntity<WebSiteConfigurationResponse> updateSiteConfiguration(WebSiteConfigurationRequest request) {
		var updatedConfiguration = webSiteConfigurationService.updateConfiguration(request);

		if (updatedConfiguration == null) {
			return ResponseEntity.notFound()
								 .build();
		}

		return ResponseEntity.ok(updatedConfiguration);
	}
}
