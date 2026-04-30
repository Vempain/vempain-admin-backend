package fi.poltsi.vempain.site.service;

import fi.poltsi.vempain.admin.api.site.request.WebSiteAclRequest;
import fi.poltsi.vempain.admin.api.site.response.WebSiteAclResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import fi.poltsi.vempain.site.entity.WebSiteAcl;
import fi.poltsi.vempain.site.entity.WebSiteFile;
import fi.poltsi.vempain.site.entity.WebSiteGallery;
import fi.poltsi.vempain.site.entity.WebSitePage;
import fi.poltsi.vempain.site.entity.WebSiteUser;
import fi.poltsi.vempain.site.repository.WebSiteAclRepository;
import fi.poltsi.vempain.site.repository.WebSiteFileRepository;
import fi.poltsi.vempain.site.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.site.repository.WebSitePageRepository;
import fi.poltsi.vempain.site.repository.WebSiteUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSiteAclServiceUTC {

	@Mock
	private WebSiteAclRepository     webSiteAclRepository;
	@Mock
	private WebSiteUserRepository    webSiteUserRepository;
	@Mock
	private WebSiteFileRepository    webSiteFileRepository;
	@Mock
	private WebSiteGalleryRepository webSiteGalleryRepository;
	@Mock
	private WebSitePageRepository    webSitePageRepository;
	@Mock
	private AccessService            accessService;

	@InjectMocks
	private WebSiteAclService webSiteAclService;

	private WebSiteAcl buildAcl(long id, long aclId, long userId) {
		return WebSiteAcl.builder()
						 .id(id)
						 .aclId(aclId)
						 .userId(userId)
						 .creator(1L)
						 .created(Instant.now())
						 .build();
	}

	private WebSiteUser buildUser(long id, String username) {
		return WebSiteUser.builder()
						  .id(id)
						  .username(username)
						  .passwordHash("hash")
						  .globalPermission(false)
						  .creator(1L)
						  .created(Instant.now())
						  .build();
	}

	@Test
	void findAllOk() {
		when(webSiteAclRepository.findAll()).thenReturn(List.of(buildAcl(1L, 10L, 5L)));

		var result = webSiteAclService.findAll();

		assertNotNull(result);
		assertEquals(1, result.size());
	}

	@Test
	void findAllEmptyOk() {
		when(webSiteAclRepository.findAll()).thenReturn(List.of());

		var result = webSiteAclService.findAll();

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void findByIdFoundOk() {
		when(webSiteAclRepository.findById(1L)).thenReturn(Optional.of(buildAcl(1L, 10L, 5L)));

		var result = webSiteAclService.findById(1L);

		assertNotNull(result);
		assertEquals(10L, result.getAclId());
		assertEquals(5L, result.getUserId());
	}

	@Test
	void findByIdNotFoundThrowsOk() {
		when(webSiteAclRepository.findById(99L)).thenReturn(Optional.empty());

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteAclService.findById(99L));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void findUsersByAclIdOk() {
		var acl = buildAcl(1L, 10L, 5L);
		var user = buildUser(5L, "alice");

		when(webSiteAclRepository.findByAclId(10L)).thenReturn(List.of(acl));
		when(webSiteUserRepository.findById(5L)).thenReturn(Optional.of(user));

		var result = webSiteAclService.findUsersByAclId(10L);

		assertNotNull(result);
		assertEquals(10L, result.getAclId());
		assertEquals(1, result.getUsers().size());
		assertEquals("alice", result.getUsers().getFirst().getUsername());
	}

	@Test
	void findUsersByAclIdNoUsersOk() {
		when(webSiteAclRepository.findByAclId(10L)).thenReturn(List.of());

		var result = webSiteAclService.findUsersByAclId(10L);

		assertNotNull(result);
		assertEquals(10L, result.getAclId());
		assertTrue(result.getUsers().isEmpty());
	}

	@Test
	void findResourcesByUserIdWithFilesGalleriesPagesOk() {
		var user = buildUser(5L, "alice");
		var siteFile = WebSiteFile.builder()
								  .id(100L)
								  .fileId(1L)
								  .aclId(10L)
								  .filePath("images/test.jpg")
								  .mimetype("image/jpeg")
								  .fileType(FileTypeEnum.IMAGE)
								  .metadata("{}")
								  .build();
		var gallery = WebSiteGallery.builder()
									.id(200L)
									.aclId(20L)
									.galleryId(1L)
									.shortname("my-gallery")
									.description("Test")
									.creator(1L)
									.created(Instant.now())
									.build();
		var page = WebSitePage.builder()
							  .id(300L)
							  .aclId(30L)
							  .pageId(1L)
							  .filePath("/home")
							  .title("Home")
							  .header("Header")
							  .body("Body")
							  .creator("admin")
							  .created(Instant.now())
							  .build();

		when(webSiteUserRepository.findById(5L)).thenReturn(Optional.of(user));
		when(webSiteAclRepository.findAclIdsByUserId(5L)).thenReturn(List.of(10L, 20L, 30L));
		when(webSiteFileRepository.findAll()).thenReturn(List.of(siteFile));
		when(webSiteGalleryRepository.findAll()).thenReturn(List.of(gallery));
		when(webSitePageRepository.findAll()).thenReturn(List.of(page));

		var result = webSiteAclService.findResourcesByUserId(5L);

		assertNotNull(result);
		assertEquals("alice", result.getUsername());
		assertEquals(3, result.getResources().size());
	}

	@Test
	void findResourcesByUserIdNoResourcesOk() {
		var user = buildUser(5L, "alice");

		when(webSiteUserRepository.findById(5L)).thenReturn(Optional.of(user));
		when(webSiteAclRepository.findAclIdsByUserId(5L)).thenReturn(List.of());
		when(webSiteFileRepository.findAll()).thenReturn(List.of());
		when(webSiteGalleryRepository.findAll()).thenReturn(List.of());
		when(webSitePageRepository.findAll()).thenReturn(List.of());

		var result = webSiteAclService.findResourcesByUserId(5L);

		assertNotNull(result);
		assertTrue(result.getResources().isEmpty());
	}

	@Test
	void findResourcesByUserIdNotFoundThrowsOk() {
		when(webSiteUserRepository.findById(99L)).thenReturn(Optional.empty());

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteAclService.findResourcesByUserId(99L));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void createOk() {
		var request = WebSiteAclRequest.builder()
									   .aclId(10L)
									   .userId(5L)
									   .build();
		var saved = buildAcl(1L, 10L, 5L);

		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteUserRepository.existsById(5L)).thenReturn(true);
		when(webSiteAclRepository.findByAclIdAndUserId(10L, 5L)).thenReturn(null);
		when(webSiteAclRepository.save(any(WebSiteAcl.class))).thenReturn(saved);

		var result = webSiteAclService.create(request);

		assertNotNull(result);
		assertEquals(10L, result.getAclId());
		assertEquals(5L, result.getUserId());
	}

	@Test
	void createUserNotFoundThrowsOk() {
		var request = WebSiteAclRequest.builder()
									   .aclId(10L)
									   .userId(99L)
									   .build();

		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteUserRepository.existsById(99L)).thenReturn(false);

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteAclService.create(request));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void createDuplicateAclThrowsOk() {
		var request = WebSiteAclRequest.builder()
									   .aclId(10L)
									   .userId(5L)
									   .build();
		var existing = buildAcl(1L, 10L, 5L);

		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteUserRepository.existsById(5L)).thenReturn(true);
		when(webSiteAclRepository.findByAclIdAndUserId(10L, 5L)).thenReturn(existing);

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteAclService.create(request));
		assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
	}

	@Test
	void deleteOk() {
		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteAclRepository.existsById(1L)).thenReturn(true);
		doNothing().when(webSiteAclRepository).deleteById(1L);

		webSiteAclService.delete(1L);

		verify(webSiteAclRepository).deleteById(1L);
	}

	@Test
	void deleteNotFoundThrowsOk() {
		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteAclRepository.existsById(99L)).thenReturn(false);

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteAclService.delete(99L));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void deleteByAclIdOk() {
		when(accessService.getValidUserId()).thenReturn(1L);
		doNothing().when(webSiteAclRepository).deleteByAclId(10L);

		webSiteAclService.deleteByAclId(10L);

		verify(webSiteAclRepository).deleteByAclId(10L);
	}
}
