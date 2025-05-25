package fi.poltsi.vempain.admin.service;

import fi.poltsi.vempain.admin.VempainMessages;
import fi.poltsi.vempain.admin.entity.Acl;
import fi.poltsi.vempain.admin.entity.Unit;
import fi.poltsi.vempain.admin.entity.UserAccount;
import fi.poltsi.vempain.admin.tools.TestUTCTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessServiceUTC {
	@Mock
	Authentication  authentication;
	@Mock
	SecurityContext securityContext;
	@Mock
	private AclService  aclService;
	@Mock
	private UserService userService;
	@Mock
	private Environment environment;

	@InjectMocks
	private AccessService accessService;

	@Test
	void hasReadPermissionOk() {
		when(environment.getProperty("vempain.test")).thenReturn("false");
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		UserAccount     userAccount = TestUTCTools.generateUser(1L);
		UserDetailsImpl userDetails = UserDetailsImpl.build(userAccount);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(userService.findById(1L)).thenReturn(Optional.of(userAccount));

		Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
		when(aclService.findAclByAclId(1L)).thenReturn(Collections.singletonList(acl));

		try {
			accessService.hasReadPermission(1L);
		} catch (Exception e) {
			fail("Should not have received an exception: " + e.getMessage());
		}
	}

	@Test
	void hasReadPermissionViaUnitOk() {
		when(environment.getProperty("vempain.test")).thenReturn("false");
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		UserAccount     userAccount = TestUTCTools.generateUser(1L);
		List<Unit>  units       = TestUTCTools.generateUnitList(3L);
		userAccount.setUnits(new HashSet<>(units));
		UserDetailsImpl userDetails = UserDetailsImpl.build(userAccount);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(userService.findById(1L)).thenReturn(Optional.of(userAccount));


		Acl acl = TestUTCTools.generateAcl(1L, 1L, null, 1L);
		when(aclService.findAclByAclId(1L)).thenReturn(Collections.singletonList(acl));

		try {
			accessService.hasReadPermission(1L);
		} catch (Exception e) {
			fail("Should not have received an exception: " + e.getMessage());
		}
	}

	@Test
	void hasReadPermissionNullAuthenticationFail() {
		when(environment.getProperty("vempain.test")).thenReturn("false");
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		UserAccount     userAccount = TestUTCTools.generateUser(1L);
		UserDetailsImpl userDetails = UserDetailsImpl.build(userAccount);

		when(securityContext.getAuthentication()).thenReturn(null);

		try {
			accessService.hasReadPermission(1L);
			fail("Should have received a SessionAuthenticationException");
		} catch (SessionAuthenticationException e) {
			assertEquals(VempainMessages.INVALID_USER_SESSION, e.getMessage());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void hasReadPermissionNoUSerFoundFail() {
		when(environment.getProperty("vempain.test")).thenReturn("false");
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		UserAccount     userAccount = TestUTCTools.generateUser(1L);
		UserDetailsImpl userDetails = UserDetailsImpl.build(userAccount);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(userService.findById(1L)).thenReturn(Optional.of(userAccount));

		when(userService.findById(1L)).thenReturn(Optional.empty());

		try {
			accessService.hasReadPermission(1L);
			fail("Should have received a SessionAuthenticationException");
		} catch (SessionAuthenticationException e) {
			assertEquals(VempainMessages.INVALID_USER_SESSION, e.getMessage());
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void hasReadPermissionNoAclFoundFail() {
		when(environment.getProperty("vempain.test")).thenReturn("false");
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		UserAccount     userAccount = TestUTCTools.generateUser(1L);
		UserDetailsImpl userDetails = UserDetailsImpl.build(userAccount);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(userService.findById(1L)).thenReturn(Optional.of(userAccount));
		when(aclService.findAclByAclId(1L)).thenReturn(new ArrayList<>());

		try {
			boolean response = accessService.hasReadPermission(1L);
			assertFalse(response);
		} catch (Exception e) {
			fail("Should not have received any other exception: " + e);
		}
	}

	@Test
	void hasModifyPermissionOk() {
		when(environment.getProperty("vempain.test")).thenReturn("false");
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		UserAccount     userAccount = TestUTCTools.generateUser(1L);
		UserDetailsImpl userDetails = UserDetailsImpl.build(userAccount);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(userService.findById(1L)).thenReturn(Optional.of(userAccount));

		try {
			accessService.hasModifyPermission(1L);
		} catch (Exception e) {
			fail("Should not have received an exception: " + e.getMessage());
		}
	}

	@Test
	void hasCreatePermissionOk() {
		when(environment.getProperty("vempain.test")).thenReturn("false");
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		UserAccount     userAccount = TestUTCTools.generateUser(1L);
		UserDetailsImpl userDetails = UserDetailsImpl.build(userAccount);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(userService.findById(1L)).thenReturn(Optional.of(userAccount));

		try {
			accessService.hasCreatePermission(1L);
		} catch (Exception e) {
			fail("Should not have received an exception: " + e.getMessage());
		}
	}

	@Test
	void hasDeletePermissionOk() {
		when(environment.getProperty("vempain.test")).thenReturn("false");
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		UserAccount     userAccount = TestUTCTools.generateUser(1L);
		UserDetailsImpl userDetails = UserDetailsImpl.build(userAccount);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(userService.findById(1L)).thenReturn(Optional.of(userAccount));

		try {
			accessService.hasDeletePermission(1L);
		} catch (Exception e) {
			fail("Should not have received an exception: " + e.getMessage());
		}
	}

	@Test
	void getUserIdOk() {
		when(environment.getProperty("vempain.test")).thenReturn("false");
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		UserAccount     userAccount = TestUTCTools.generateUser(1L);
		UserDetailsImpl userDetails = UserDetailsImpl.build(userAccount);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(userService.findById(1L)).thenReturn(Optional.of(userAccount));

		try {
			accessService.getUserId();
		} catch (Exception e) {
			fail("Should not have received an exception: " + e.getMessage());
		}
	}

	@Test
	void getUserIdNoUserFail() {
		when(environment.getProperty("vempain.test")).thenReturn("false");
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		UserAccount     userAccount = TestUTCTools.generateUser(1L);
		UserDetailsImpl userDetails = UserDetailsImpl.build(userAccount);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(userService.findById(1L)).thenReturn(Optional.empty());

		try {
			accessService.getUserId();
			fail("We should have received SessionAuthenticationException with non-existing user");
		} catch (SessionAuthenticationException e) {
			assertEquals(VempainMessages.INVALID_USER_SESSION, e.getMessage());
		} catch (Exception e) {
			fail("Should only have received SessionAuthenticationException exception: " + e.getMessage());
		}
	}

	@Test
	void checkAuthenticationOk() {
		when(environment.getProperty("vempain.test")).thenReturn("false");
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		UserAccount     userAccount = TestUTCTools.generateUser(1L);
		UserDetailsImpl userDetails = UserDetailsImpl.build(userAccount);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(userService.findById(1L)).thenReturn(Optional.of(userAccount));

		try {
			accessService.checkAuthentication();
		} catch (Exception e) {
			fail("Should not have received an exception: " + e.getMessage());
		}
	}

	@Test
	void checkAuthenticationNoUserFail() {
		when(environment.getProperty("vempain.test")).thenReturn("false");
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		UserAccount     userAccount = TestUTCTools.generateUser(1L);
		UserDetailsImpl userDetails = UserDetailsImpl.build(userAccount);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(userService.findById(1L)).thenReturn(Optional.of(userAccount));
		when(userService.findById(1L)).thenReturn(Optional.empty());

		try {
			accessService.checkAuthentication();
			fail("Should have thrown aResponseStatusException");
		} catch (ResponseStatusException e) {
			assertEquals("403 FORBIDDEN \"User must be logged on to use this resource\"", e.getMessage());
			assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
		} catch (Exception e) {
			fail("Should not have received an exception: " + e);
		}
	}

	@Test
	void aclListContainsPermissionOk() {
		List<Boolean> permissionList = Arrays.asList(true, false, false, false);
		UserAccount   userAccount    = TestUTCTools.generateUser(1L);
		List<Acl>     acls           = TestUTCTools.generateAclList(1L, 4L);
		assertTrue(accessService.aclListContainsPermission(permissionList, userAccount, acls));
	}

	@Test
	void aclListContainsPermissionNoneFail() {
		List<Boolean> permissionList = Arrays.asList(true, false, false, false);
		UserAccount   userAccount    = TestUTCTools.generateUser(5L);
		List<Acl>     acls           = TestUTCTools.generateAclList(1L, 4L);
		assertFalse(accessService.aclListContainsPermission(permissionList, userAccount, acls));
	}

	@Test
	void aclListContainsPermissionNoneListFail() {
		List<Boolean> permissionList = Arrays.asList(false, false, false, false);
		UserAccount   userAccount    = TestUTCTools.generateUser(5L);
		List<Acl>     acls           = TestUTCTools.generateAclList(1L, 4L);
		assertFalse(accessService.aclListContainsPermission(permissionList, userAccount, acls));
	}

	@Test
	void aclListContainsPermissionUnitOk() {
		List<Boolean> permissionList = Arrays.asList(true, false, false, false);
		UserAccount   userAccount    = TestUTCTools.generateUser(5L);
		Unit          unit           = TestUTCTools.generateUnit(1L);
		userAccount.getUnits().add(unit);
		List<Acl> acls = TestUTCTools.generateAclList(1L, 4L);
		assertTrue(accessService.aclListContainsPermission(permissionList, userAccount, acls));
	}

	@Test
	void aclListContainsPermissionNoUserPermissionFail() {
		testPermissions(Arrays.asList(true, false, false, false),
						Arrays.asList(false, true, false, false),
						Arrays.asList(false, true, false, false),
						1L, 1L, false);
	}

	@Test
	void aclListContainsPermissionUnitPermissionOk() {
		testPermissions(Arrays.asList(true, false, false, false),
						Arrays.asList(false, true, false, false),
						Arrays.asList(true, false, false, false),
                        1L, 1L, true);
	}

	@Test
	void aclListContainsPermissionUnitPermissionFail() {
		testPermissions(Arrays.asList(true, false, false, false),
						Arrays.asList(false, true, false, false),
						Arrays.asList(false, true, false, false),
                        1L, 1L, false);
	}

	@Test
	void aclListContainsPermissionNoUnitPermissionFail() {
        testPermissions(Arrays.asList(true, false, false, false),
                        Arrays.asList(false, true, false, false),
                        Arrays.asList(false, true, false, false),
                        1L, 8L, false);
	}

	@Test
	void hasPermissionsOk() {
		Acl         acl  = TestUTCTools.generateAcl(1L, 1L, 1L, null);
		List<Boolean> mask = Arrays.asList(false, false, false, false);

		for (int i = 0; i < 4; i++) {
			mask.set(i, true);
			acl.setReadPrivilege(false);
			acl.setModifyPrivilege(false);
			acl.setCreatePrivilege(false);
			acl.setDeletePrivilege(false);

			switch (i) {
				case 0:
					acl.setReadPrivilege(true);
					break;
				case 1:
					acl.setModifyPrivilege(true);
					break;
				case 2:
					acl.setCreatePrivilege(true);
					break;
				case 3:
					acl.setDeletePrivilege(true);
					break;
			}

			assertTrue(accessService.hasPermissions(acl, mask));

			mask.set(i, false);
		}
	}

	@Test
	void hasPermissionsNoneFail() {
		Acl         acl  = TestUTCTools.generateAcl(1L, 1L, 1L, null);
		List<Boolean> mask = Arrays.asList(false, false, false, false);

		acl.setReadPrivilege(false);
		acl.setModifyPrivilege(false);
		acl.setCreatePrivilege(false);
		acl.setDeletePrivilege(false);

		for (int i = 0; i < 4; i++) {
			mask.set(i, true);
			assertFalse(accessService.hasPermissions(acl, mask));
			mask.set(i, false);
		}

		for (int i = 0; i < 4; i++) {
			mask.set(i, true);
			assertFalse(accessService.hasPermissions(acl, mask));
		}
	}

	private void testPermissions(List<Boolean> permissionList, List<Boolean> userPermissions, List<Boolean> groupPermissions,
								 long userId, long unitId, boolean expectedResult) {
		UserAccount userAccount = TestUTCTools.generateUser(userId);
		Unit        unit        = TestUTCTools.generateUnit(unitId);
		userAccount.getUnits().add(unit);
		List<Acl> acls = TestUTCTools.generateAclList(1L, 1L);

		// This is the user-specific ACL
		acls.getFirst().setReadPrivilege(userPermissions.get(0));
		acls.getFirst().setModifyPrivilege(userPermissions.get(1));
		acls.get(0).setCreatePrivilege(userPermissions.get(2));
		acls.get(0).setDeletePrivilege(userPermissions.get(3));
		// This is the unit-specific ACL
		acls.get(1).setReadPrivilege(groupPermissions.get(0));
		acls.get(1).setModifyPrivilege(groupPermissions.get(1));
		acls.get(1).setCreatePrivilege(groupPermissions.get(2));
		acls.get(1).setDeletePrivilege(groupPermissions.get(3));
		assertEquals(expectedResult, accessService.aclListContainsPermission(permissionList, userAccount, acls));
	}
}
