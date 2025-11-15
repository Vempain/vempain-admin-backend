package fi.poltsi.vempain.site.service;

import fi.poltsi.vempain.admin.api.site.request.WebSiteUserRequest;
import fi.poltsi.vempain.admin.api.site.response.WebSiteUserResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.site.entity.WebSiteUser;
import fi.poltsi.vempain.site.repository.WebSiteUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSiteUserService {
	private final WebSiteUserRepository webSiteUserRepository;
	private final PasswordEncoder       passwordEncoder;
	private final AccessService         accessService;

	/**
	 * Find all site web users
	 *
	 * @return List of user responses
	 */
	public List<WebSiteUserResponse> findAll() {
		List<WebSiteUserResponse> responses = new ArrayList<>();
		webSiteUserRepository.findAll()
							 .forEach(user -> responses.add(user.toResponse()));
		return responses;
	}

	/**
	 * Find a site web user by ID
	 *
	 * @param userId The user ID
	 * @return User response
	 */
	public WebSiteUserResponse findById(Long userId) {
		return webSiteUserRepository.findById(userId)
									.map(WebSiteUser::toResponse)
									.orElseThrow(() -> {
										log.error("Site web user not found with ID: {}", userId);
										return new ResponseStatusException(HttpStatus.NOT_FOUND, "Site web user not found");
									});
	}

	/**
	 * Find a site web user by username
	 *
	 * @param username The username
	 * @return User response
	 */
	public WebSiteUserResponse findByUsername(String username) {
		return webSiteUserRepository.findByUsername(username)
									.map(WebSiteUser::toResponse)
									.orElseThrow(() -> {
										log.error("Site web user not found with username: {}", username);
										return new ResponseStatusException(HttpStatus.NOT_FOUND, "Site web user not found");
									});
	}

	/**
	 * Create a new site web user
	 *
	 * @param request The user request containing username and password
	 * @return Created user response
	 */
	@Transactional
	public WebSiteUserResponse create(WebSiteUserRequest request) {
		Long adminUserId = accessService.getValidUserId();

		// Validate password is provided for new user
		if (request.getPassword() == null || request.getPassword()
													.isBlank()) {
			log.error("Password is required when creating a new site web user");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
		}

		// Check if username already exists
		if (webSiteUserRepository.existsByUsername(request.getUsername())) {
			log.error("Username already exists: {}", request.getUsername());
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
		}

		String passwordHash = passwordEncoder.encode(request.getPassword());

		WebSiteUser user = WebSiteUser.builder()
									  .username(request.getUsername())
									  .passwordHash(passwordHash)
									  .creator(adminUserId)
									  .created(Instant.now())
									  .build();

		WebSiteUser saved = webSiteUserRepository.save(user);
		log.info("Created new site web user with ID: {} by admin user: {}", saved.getId(), adminUserId);
		return saved.toResponse();
	}

	/**
	 * Update an existing site web user
	 *
	 * @param userId  The user ID to update
	 * @param request The user request containing updated data
	 * @return Updated user response
	 */
	@Transactional
	public WebSiteUserResponse update(Long userId, WebSiteUserRequest request) {
		Long adminUserId = accessService.getValidUserId();

		WebSiteUser user = webSiteUserRepository.findById(userId)
												.orElseThrow(() -> {
													log.error("Site web user not found with ID: {}", userId);
													return new ResponseStatusException(HttpStatus.NOT_FOUND, "Site web user not found");
												});

		// Update username if changed and not already taken
		if (!user.getUsername()
				 .equals(request.getUsername())) {
			if (webSiteUserRepository.existsByUsername(request.getUsername())) {
				log.error("Username already exists: {}", request.getUsername());
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
			}
			user.setUsername(request.getUsername());
		}

		// Update password if provided
		if (request.getPassword() != null && !request.getPassword()
													 .isBlank()) {
			String passwordHash = passwordEncoder.encode(request.getPassword());
			user.setPasswordHash(passwordHash);
		}

		user.setModifier(adminUserId);
		user.setModified(Instant.now());

		WebSiteUser saved = webSiteUserRepository.save(user);
		log.info("Updated site web user ID: {} by admin user: {}", userId, adminUserId);
		return saved.toResponse();
	}

	/**
	 * Delete a site web user
	 * Note: This will cascade delete all ACL entries for this user
	 *
	 * @param userId The user ID to delete
	 */
	@Transactional
	public void delete(Long userId) {
		Long adminUserId = accessService.getValidUserId();

		if (!webSiteUserRepository.existsById(userId)) {
			log.error("Site web user not found with ID: {}", userId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Site web user not found");
		}

		webSiteUserRepository.deleteById(userId);
		log.info("Deleted site web user ID: {} by admin user: {}", userId, adminUserId);
	}

	/**
	 * Change password for a site web user
	 *
	 * @param userId      The user ID
	 * @param newPassword The new plaintext password
	 * @return Updated user response
	 */
	@Transactional
	public WebSiteUserResponse changePassword(Long userId, String newPassword) {
		Long adminUserId = accessService.getValidUserId();

		if (newPassword == null || newPassword.isBlank()) {
			log.error("Password cannot be empty");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty");
		}

		WebSiteUser user = webSiteUserRepository.findById(userId)
												.orElseThrow(() -> {
													log.error("Site web user not found with ID: {}", userId);
													return new ResponseStatusException(HttpStatus.NOT_FOUND, "Site web user not found");
												});

		String passwordHash = passwordEncoder.encode(newPassword);
		user.setPasswordHash(passwordHash);
		user.setModifier(adminUserId);
		user.setModified(Instant.now());

		WebSiteUser saved = webSiteUserRepository.save(user);
		log.info("Changed password for site web user ID: {} by admin user: {}", userId, adminUserId);
		return saved.toResponse();
	}
}

