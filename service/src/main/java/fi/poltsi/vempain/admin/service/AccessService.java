package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.auth.entity.AbstractVempainEntity;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import fi.poltsi.vempain.auth.service.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resource authorization for the admin backend. Every check is evaluated against the ACL rows of the resource's {@code acl_id} for the
 * authenticated user and the user's units. There is no role based fallback and no test-mode bypass: tests must authenticate a real
 * principal and create ACL rows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessService {
	private static final List<Boolean> READ_PRIVILEGE   = Arrays.asList(true, false, false, false);
	private static final List<Boolean> MODIFY_PRIVILEGE = Arrays.asList(false, true, false, false);
	private static final List<Boolean> CREATE_PRIVILEGE = Arrays.asList(false, false, true, false);
	private static final List<Boolean> DELETE_PRIVILEGE = Arrays.asList(false, false, false, true);
	private final        AclService    aclService;
	private final        UserService   userService;

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

	/**
	 * JPA specification that keeps only the ACL-linked entities the current user may read, so that paged listings are filtered and
	 * counted in the database. Entities whose {@code acl_id} is not positive, or whose ACL rows are missing, are never returned.
	 * Without an authenticated Vempain user the specification matches nothing.
	 */
	public <T extends AbstractVempainEntity> Specification<T> readableSpecification() {
		var user = getUser();

		if (user == null) {
			return (root, query, criteriaBuilder) -> criteriaBuilder.disjunction();
		}

		var userId = user.getId();
		Set<Long> unitIds = user.getUnits() == null
							? Set.of()
							: user.getUnits()
								  .stream()
								  .map(Unit::getId)
								  .filter(Objects::nonNull)
								  .collect(Collectors.toSet());

		return (root, query, criteriaBuilder) -> {
			var accessibleAclIds = query.subquery(Long.class);
			var accessibleAclRoot = accessibleAclIds.from(Acl.class);
			accessibleAclIds.select(accessibleAclRoot.get("aclId"));

			var principalPredicates = new ArrayList<Predicate>();
			principalPredicates.add(criteriaBuilder.equal(accessibleAclRoot.get("userId"), userId));
			if (!unitIds.isEmpty()) {
				principalPredicates.add(accessibleAclRoot.get("unitId")
														 .in(unitIds));
			}

			accessibleAclIds.where(
					criteriaBuilder.equal(accessibleAclRoot.get("aclId"), root.get("aclId")),
					criteriaBuilder.isTrue(accessibleAclRoot.get("readPrivilege")),
					criteriaBuilder.or(principalPredicates.toArray(Predicate[]::new))
			);

			return criteriaBuilder.and(
					criteriaBuilder.greaterThan(root.get("aclId"), 0L),
					criteriaBuilder.exists(accessibleAclIds)
			);
		};
	}

	public Long getUserId() {
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
	 * Require modify access to the reserved administrator ACL used by
	 * administration endpoints that do not belong to a content ACL.
	 */
	public void checkAdminAccess() {
		try {
			if (!hasModifyPermission(Constants.ADMIN_ID)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required");
			}
		} catch (SessionAuthenticationException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, VempainMessages.INVALID_USER_SESSION);
		}
	}

	/**
	 * Returns the user ID of the currently authenticated user.
	 * If the user is not authenticated, a frontend compatible exception is thrown.
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
		var user = getUser();

		if (user == null) {
			throw new SessionAuthenticationException(VempainMessages.INVALID_USER_SESSION);
		}

		if (aclId < 1) {
			return false;
		}

		List<Acl> acls = aclService.findAclByAclId(aclId);

		if (acls.isEmpty()) {
			return false;
		}

		return aclListContainsPermission(permissionList, user, acls);
	}

	protected boolean aclListContainsPermission(List<Boolean> permissionList, UserAccount userAccount, List<Acl> acls) {
		Set<Unit> units = userAccount.getUnits() == null ? Set.of() : userAccount.getUnits();

		for (Acl acl : acls) {
			if (acl.getUserId() != null &&
			    acl.getUserId()
			       .equals(userAccount.getId())
			    && hasPermissions(acl, permissionList)) {
				return true;
			} else if (acl.getUnitId() != null) {
				for (Unit unit : units) {
					if (unit.getId()
							.equals(acl.getUnitId())
						&& hasPermissions(acl, permissionList)) {
						return true;
					}
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
				log.error("Failed to fetch the authenticated user principal");
				return null;
			}

			if (userDetails == null || userDetails.getId() == null) {
				return null;
			}

			Optional<UserAccount> user = userService.findById(userDetails.getId());

			return user.orElse(null);
		}

		return null;
	}
}
