package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.api.response.AclResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.admin.service.AclService;
import fi.poltsi.vempain.admin.tools.MockServiceTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

class AclControllerUTC {
    static final long count = 10L;

    @Mock
    private AclService aclService;
    @Mock
    private AccessService accessService;

    private AclController aclController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aclController = new AclController(aclService, accessService);
    }

    @Test
    void getAllAclOk() {
        doNothing().when(accessService).checkAuthentication();
        MockServiceTools.aclServiceFindAllOk(aclService, count);

        ResponseEntity<List<AclResponse>> aclResponseEntity = aclController.getAllAcl();
        assertNotNull(aclResponseEntity);
        List<AclResponse> aclResponses = aclResponseEntity.getBody();
        assertNotNull(aclResponses);
        assertEquals(8 * count, aclResponses.size());
    }

    @Test
    void getAllAclOkNoAcl() {
        doNothing().when(accessService).checkAuthentication();
        MockServiceTools.aclServiceFindAllOk(aclService, 0);

        ResponseEntity<List<AclResponse>> aclResponseEntity = aclController.getAllAcl();
        assertNotNull(aclResponseEntity);
        List<AclResponse> aclResponses = aclResponseEntity.getBody();
        assertNotNull(aclResponses);
        assertEquals(0, aclResponses.size());
    }

    @Test
    void getAllAclFailNoAccess() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User must be logged on to use this resource")).when(accessService).checkAuthentication();
        MockServiceTools.aclServiceFindAllOk(aclService, count);

        try {
            aclController.getAllAcl();
            fail("We should have gotten an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
            assertEquals("403 FORBIDDEN \"User must be logged on to use this resource\"", e.getMessage());
        }
    }

    @Test
    void getAclOk() {
        doNothing().when(accessService).checkAuthentication();
        MockServiceTools.aclServicefindAclByAclIdOk(aclService, 1L);
        ResponseEntity<List<AclResponse>> responseEntity = aclController.getAcl(1L);
        assertNotNull(responseEntity);
        List<AclResponse> aclResponses = responseEntity.getBody();
        assertNotNull(aclResponses);
        assertEquals(8, aclResponses.size());
    }

    @Test
    void getAclFailNoAccess() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User must be logged on to use this resource")).when(accessService).checkAuthentication();
        MockServiceTools.aclServicefindAclByAclIdOk(aclService, 1L);

        try {
            aclController.getAcl(1L);
            fail("We should have gotten an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
            assertEquals("403 FORBIDDEN \"User must be logged on to use this resource\"", e.getMessage());
        }
    }

    @Test
    void getAclFailNoAcl() {
        doNothing().when(accessService).checkAuthentication();
        MockServiceTools.aclServicefindAclByAclIdEmptyList(aclService, 1L);

        try {
            aclController.getAcl(1L);
            fail("We should have gotten an exception");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
            assertEquals("404 NOT_FOUND \"There are no ACL in the database\"", e.getMessage());
        }
    }
}
