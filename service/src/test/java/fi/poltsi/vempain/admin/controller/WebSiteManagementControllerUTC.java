package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.api.site.request.WebSiteAclRequest;
import fi.poltsi.vempain.admin.api.site.request.WebSiteConfigurationRequest;
import fi.poltsi.vempain.admin.api.site.request.WebSiteResourcePagedRequest;
import fi.poltsi.vempain.admin.api.site.request.WebSiteUserRequest;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.site.service.WebSiteAclService;
import fi.poltsi.vempain.site.service.WebSiteConfigurationService;
import fi.poltsi.vempain.site.service.WebSiteResourceService;
import fi.poltsi.vempain.site.service.WebSiteUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSiteManagementControllerUTC {
	@Mock
	private WebSiteUserService          userService;
	@Mock
	private WebSiteAclService           aclService;
	@Mock
	private AccessService               accessService;
	@Mock
	private WebSiteResourceService      resourceService;
	@Mock
	private WebSiteConfigurationService configurationService;
	@InjectMocks
	private WebSiteManagementController controller;

	@Test
	void delegatesAuthenticatedUserAclAndResourceOperations() {
		var userRequest = new WebSiteUserRequest();
		var aclRequest = new WebSiteAclRequest();
		var resourceRequest = new WebSiteResourcePagedRequest();
		when(userService.findAll()).thenReturn(List.of());
		when(aclService.findAll()).thenReturn(List.of());
		when(resourceService.listResources(resourceRequest)).thenReturn(null);

		assertEquals(200, controller.getAllUsers()
		                            .getStatusCode()
		                            .value());
		assertNull(controller.getUserById(1L)
		                     .getBody());
		assertNull(controller.createUser(userRequest)
		                     .getBody());
		assertNull(controller.updateUser(1L, userRequest)
		                     .getBody());
		assertEquals(204, controller.deleteUser(1L)
		                            .getStatusCode()
		                            .value());
		assertEquals(200, controller.getAllAcls()
		                            .getStatusCode()
		                            .value());
		assertNull(controller.getUsersByAclId(1L)
		                     .getBody());
		assertNull(controller.getResourcesByUserId(1L)
		                     .getBody());
		assertNull(controller.createAcl(aclRequest)
		                     .getBody());
		assertEquals(204, controller.deleteAcl(1L)
		                            .getStatusCode()
		                            .value());
		assertNull(controller.getResources(resourceRequest)
		                     .getBody());
		verify(accessService, org.mockito.Mockito.times(11)).checkAuthentication();
	}

	@Test
	void handlesExistingAndMissingSiteConfiguration() {
		var request = new WebSiteConfigurationRequest();
		when(configurationService.getAllConfigurations()).thenReturn(List.of());
		when(configurationService.getConfigurationById(1L)).thenReturn(null);
		when(configurationService.updateConfiguration(request)).thenReturn(null);

		assertEquals(200, controller.getAllSiteConfigurations()
		                            .getStatusCode()
		                            .value());
		assertEquals(404, controller.getSiteConfigurationById(1L)
		                            .getStatusCode()
		                            .value());
		assertEquals(404, controller.updateSiteConfiguration(request)
		                            .getStatusCode()
		                            .value());
	}
}
