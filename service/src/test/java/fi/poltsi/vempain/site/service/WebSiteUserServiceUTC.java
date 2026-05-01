package fi.poltsi.vempain.site.service;

import fi.poltsi.vempain.admin.api.site.request.WebSiteUserRequest;
import fi.poltsi.vempain.admin.api.site.response.WebSiteUserResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.site.entity.WebSiteUser;
import fi.poltsi.vempain.site.repository.WebSiteUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSiteUserServiceUTC {

	@Mock
	private PasswordEncoder       passwordEncoder;
	@Mock
	private AccessService         accessService;
	@Mock
	private WebSiteAclService     webSiteAclService;
	@Mock
	private WebSiteUserRepository webSiteUserRepository;

	@InjectMocks
	private WebSiteUserService webSiteUserService;

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

	private WebSiteUserResponse buildUserResponse(long id, String username) {
		return WebSiteUserResponse.builder()
								  .id(id)
								  .username(username)
								  .creator(1L)
								  .created(Instant.now())
								  .globalPermission(false)
								  .build();
	}

	@Test
	void findAllOk() {
		var user1 = buildUser(1L, "alice");
		var user2 = buildUser(2L, "bob");
		var resp1 = buildUserResponse(1L, "alice");
		var resp2 = buildUserResponse(2L, "bob");

		when(webSiteUserRepository.findAll()).thenReturn(List.of(user1, user2));
		when(webSiteAclService.findResourcesByUserId(1L)).thenReturn(resp1);
		when(webSiteAclService.findResourcesByUserId(2L)).thenReturn(resp2);

		var result = webSiteUserService.findAll();

		assertNotNull(result);
		assertEquals(2, result.size());
	}

	@Test
	void findByIdFoundOk() {
		var user = buildUser(1L, "alice");
		var resp = buildUserResponse(1L, "alice");

		when(webSiteUserRepository.findById(1L)).thenReturn(Optional.of(user));
		when(webSiteAclService.findResourcesByUserId(1L)).thenReturn(resp);

		var result = webSiteUserService.findById(1L);

		assertNotNull(result);
		assertEquals("alice", result.getUsername());
	}

	@Test
	void findByIdNotFoundThrowsOk() {
		when(webSiteUserRepository.findById(99L)).thenReturn(Optional.empty());

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteUserService.findById(99L));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void findByUsernameFoundOk() {
		var user = buildUser(1L, "alice");
		when(webSiteUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));

		var result = webSiteUserService.findByUsername("alice");

		assertNotNull(result);
		assertEquals("alice", result.getUsername());
	}

	@Test
	void findByUsernameNotFoundThrowsOk() {
		when(webSiteUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteUserService.findByUsername("ghost"));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void createOk() {
		var request = WebSiteUserRequest.builder()
										.username("newuser")
										.password("password123")
										.globalPermission(false)
										.build();
		var saved = buildUser(10L, "newuser");

		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteUserRepository.existsByUsername("newuser")).thenReturn(false);
		when(webSiteUserRepository.save(any(WebSiteUser.class))).thenReturn(saved);

		var result = webSiteUserService.create(request);

		assertNotNull(result);
		assertEquals("newuser", result.getUsername());
	}

	@Test
	void createMissingPasswordThrowsOk() {
		var request = WebSiteUserRequest.builder()
										.username("newuser")
										.password(null)
										.globalPermission(false)
										.build();
		when(accessService.getValidUserId()).thenReturn(1L);

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteUserService.create(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void createBlankPasswordThrowsOk() {
		var request = WebSiteUserRequest.builder()
										.username("newuser")
										.password("   ")
										.globalPermission(false)
										.build();
		when(accessService.getValidUserId()).thenReturn(1L);

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteUserService.create(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void createDuplicateUsernameThrowsOk() {
		var request = WebSiteUserRequest.builder()
										.username("existing")
										.password("pass")
										.globalPermission(false)
										.build();
		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteUserRepository.existsByUsername("existing")).thenReturn(true);

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteUserService.create(request));
		assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
	}

	@Test
	void updateOk() {
		var request = WebSiteUserRequest.builder()
										.username("alice")
										.password("newpass")
										.globalPermission(true)
										.build();
		var existing = buildUser(1L, "alice");
		var saved = buildUser(1L, "alice");
		saved.setGlobalPermission(true);

		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteUserRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(webSiteUserRepository.save(any(WebSiteUser.class))).thenReturn(saved);

		var result = webSiteUserService.update(1L, request);

		assertNotNull(result);
	}

	@Test
	void updateUsernameChangedNoConflictOk() {
		var request = WebSiteUserRequest.builder()
										.username("alice-new")
										.password(null)
										.globalPermission(false)
										.build();
		var existing = buildUser(1L, "alice");
		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteUserRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(webSiteUserRepository.existsByUsername("alice-new")).thenReturn(false);
		when(webSiteUserRepository.save(any(WebSiteUser.class))).thenReturn(existing);

		var result = webSiteUserService.update(1L, request);

		assertNotNull(result);
	}

	@Test
	void updateUsernameConflictThrowsOk() {
		var request = WebSiteUserRequest.builder()
										.username("bob")
										.password(null)
										.globalPermission(false)
										.build();
		var existing = buildUser(1L, "alice");
		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteUserRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(webSiteUserRepository.existsByUsername("bob")).thenReturn(true);

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteUserService.update(1L, request));
		assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
	}

	@Test
	void updateNotFoundThrowsOk() {
		var request = WebSiteUserRequest.builder()
										.username("ghost")
										.password("pass")
										.globalPermission(false)
										.build();
		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteUserRepository.findById(99L)).thenReturn(Optional.empty());

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteUserService.update(99L, request));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void deleteOk() {
		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteUserRepository.existsById(1L)).thenReturn(true);
		doNothing().when(webSiteUserRepository).deleteById(1L);

		webSiteUserService.delete(1L);

		verify(webSiteUserRepository).deleteById(1L);
	}

	@Test
	void deleteNotFoundThrowsOk() {
		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteUserRepository.existsById(99L)).thenReturn(false);

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteUserService.delete(99L));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void changePasswordOk() {
		var user = buildUser(1L, "alice");
		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteUserRepository.findById(1L)).thenReturn(Optional.of(user));
		when(webSiteUserRepository.save(any(WebSiteUser.class))).thenReturn(user);

		var result = webSiteUserService.changePassword(1L, "newSecurePassword");

		assertNotNull(result);
	}

	@Test
	void changePasswordNullThrowsOk() {
		when(accessService.getValidUserId()).thenReturn(1L);

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteUserService.changePassword(1L, null));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void changePasswordBlankThrowsOk() {
		when(accessService.getValidUserId()).thenReturn(1L);

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteUserService.changePassword(1L, "   "));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void changePasswordUserNotFoundThrowsOk() {
		when(accessService.getValidUserId()).thenReturn(1L);
		when(webSiteUserRepository.findById(99L)).thenReturn(Optional.empty());

		var ex = assertThrows(ResponseStatusException.class,
							  () -> webSiteUserService.changePassword(99L, "validpass"));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}
}
