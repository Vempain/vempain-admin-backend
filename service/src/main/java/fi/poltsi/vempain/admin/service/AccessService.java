package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.entity.Acl;
import fi.poltsi.vempain.admin.entity.Unit;
import fi.poltsi.vempain.admin.entity.UserAccount;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@AllArgsConstructor
public class AccessService {
	private static final List<Boolean> READ_PRIVILEGE   = Arrays.asList(true, false, false, false);
	private static final List<Boolean> MODIFY_PRIVILEGE = Arrays.asList(false, true, false, false);
	private static final List<Boolean> CREATE_PRIVILEGE = Arrays.asList(false, false, true, false);
	private static final List<Boolean> DELETE_PRIVILEGE = Arrays.asList(false, false, false, true);
	private final        AclService    aclService;
	private final        UserService   userService;
	private final        Environment   environment;

	public boolean hasReadPermission(long aclId) {
		return hasPermission(aclId, READ_PRIVILEGE);
	}

	public boolean hasModifyPermission(long aclId) {
		return hasPermission(aclId, MODIFY_PRIVILEGE);
	}

	public boolean hasCreatePermission(long aclId) {
		return hasPermission(aclId, CREATE_PRIVILEGE);
	}

	public boolean hasDeletePermission(long aclId) {
		return hasPermission(aclId, DELETE_PRIVILEGE);
	}

	public Long getUserId() {
		// If we're running as a ITC, we return the first user ID we can find in database
		if (Objects.requireNonNull(environment.getProperty("vempain.test"))
				   .equalsIgnoreCase("true")) {
			var users = userService.findAll();

			if (StreamSupport.stream(users.spliterator(), false)
							 .findAny()
							 .isPresent()) {
				return users.iterator()
							.next()
							.getId();
			} else {
				log.error("No user found in database, have you set up the test correctly?");
				// Probably the test has not been set up properly
				throw new SessionAuthenticationException(VempainMessages.INVALID_USER_SESSION);
			}
		}

		var user = getUser();

		if (user == null) {
			throw new SessionAuthenticationException(VempainMessages.INVALID_USER_SESSION);
		}

		return user.getId();
	}

	public void checkAuthentication() {
		try {
			getUserId();
		} catch (SessionAuthenticationException e) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User must be logged on to use this resource");
		}
	}

/**
	 * Returns the user ID of the currently authenticated user.
	 * If the user is not authenticated, an frontend compatible exception is thrown.
	 *
	 * @return the user ID of the authenticated user
	 * @throws ResponseStatusException if the user session is invalid
	 */
	public long getValidUserId() {
		long userId;
		try {
			userId = getUserId();
		} catch (SessionAuthenticationException se) {
			log.error(VempainMessages.INVALID_USER_SESSION_MSG);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.INVALID_USER_SESSION);
		}
		return userId;
	}

	private boolean hasPermission(long aclId, List<Boolean> permissionList) {
		if (Objects.requireNonNull(environment.getProperty("vempain.test"))
				   .equalsIgnoreCase("true")) {
			return true;
		}

		var user = getUser();

		if (user == null) {
			throw new SessionAuthenticationException(VempainMessages.INVALID_USER_SESSION);
		}

		List<Acl> acls = aclService.findAclByAclId(aclId);

		if (acls.isEmpty()) {
			return false;
		}

		return aclListContainsPermission(permissionList, user, acls);
	}

	protected boolean aclListContainsPermission(List<Boolean> permissionList, UserAccount userAccount, List<Acl> acls) {
		for (Acl acl : acls) {
			if (acl.getUserId() != null &&
				acl.getUserId()
				   .equals(userAccount.getId())
				&& hasPermissions(acl, permissionList)) {
				return true;
			} else if (acl.getUnitId() != null) {
				for (Unit unit : userAccount.getUnits()) {
					return unit.getId()
							   .equals(acl.getUnitId())
						   && hasPermissions(acl, permissionList);
				}
			}
		}

		return false;
	}

	protected boolean hasPermissions(Acl acl, List<Boolean> mask) {
		var result = !mask.get(0) || (acl.isReadPrivilege());
		result &= !mask.get(1) || (acl.isModifyPrivilege());
		result &= !mask.get(2) || (acl.isCreatePrivilege());
		result &= !mask.get(3) || (acl.isDeletePrivilege());
		return result;
	}

	private UserAccount getUser() {
		var auth = SecurityContextHolder.getContext()
										.getAuthentication();

		if (auth != null) {
			UserDetailsImpl userDetails;

			try {
				userDetails = (UserDetailsImpl) auth.getPrincipal();
			} catch (Exception e) {
				log.error("Failed to fetch authorisation principal from {}", auth);
				return null;
			}

			Optional<UserAccount> user = userService.findById(userDetails.getId());

			return user.orElse(null);
		}

		return null;
	}
}
