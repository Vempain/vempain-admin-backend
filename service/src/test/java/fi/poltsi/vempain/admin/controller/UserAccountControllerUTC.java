package fi.poltsi.vempain.admin.controller;

import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.api.response.UserResponse;
import fi.poltsi.vempain.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountControllerUTC {
    @Mock
    private UserService userService;

	@InjectMocks
    private UserController userController;

    @Test
    void getUsersOk() {
        when(userService.findAll()).thenReturn(TestUTCTools.generateUserList(5L));

        try {
            ResponseEntity<List<UserResponse>> responseEntity = userController.getUsers();
            assertNotNull(responseEntity);
            List<UserResponse> userResponses = responseEntity.getBody();
            assertNotNull(userResponses);
            assertEquals(5L, userResponses.size());
        } catch (Exception e) {
            fail("Should not have received an exception: " + e);
        }
    }

    @Test
    void getUsersNoneFoundOk() {
        when(userService.findAll()).thenReturn(new ArrayList<>());

        try {
            ResponseEntity<List<UserResponse>> responseEntity = userController.getUsers();
            assertNotNull(responseEntity);
            List<UserResponse> userResponses = responseEntity.getBody();
            assertNotNull(userResponses);
            assertEquals(0, userResponses.size());
        } catch (Exception e) {
            fail("Should not have received an exception: " + e);
        }
    }

    @Test
    void findByIdOk() {
        when(userService.findUserResponseById(1L)).thenReturn(TestUTCTools.generateUser(1L).getUserResponse());

        try {
            ResponseEntity<UserResponse> responseEntity = userController.findById(1L);
            assertNotNull(responseEntity);
            UserResponse userResponse = responseEntity.getBody();
            assertNotNull(userResponse);
            assertEquals(1L, userResponse.getId());
        } catch (Exception e) {
            fail("Should not have received an exception: " + e);
        }
    }

    @Test
    void findByIdNegativeIdFail() {
        findByIdFail(-1L);
    }

    @Test
    void findByIdNullIdFail() {
        findByIdFail(null);
    }

    private void findByIdFail(Long unitId) {
        try {
            userController.findById(unitId);
            fail("Should have received a ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals("400 BAD_REQUEST \"Malformed parameter\"", e.getMessage());
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }
    @Test
    void findByIdNoneFoundFail() {
        try {
            userController.findById(1L);
            fail("Should have received a ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals("404 NOT_FOUND \"No user was found with given ID\"", e.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        } catch (Exception e) {
            fail("Should not have received any other exception: " + e);
        }
    }

    @Test
    void handleRuntimeExceptionsOk() {
        try {
            ResponseEntity<Exception> responseEntity = userController.handleRuntimeExceptions(new NullPointerException("Test exception"));
            assertNotNull(responseEntity);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
            Exception e = responseEntity.getBody();
            assertNotNull(e);
            assertEquals("Test exception", e.getMessage());
        } catch (Exception e) {
            fail("Should not have received an exception: " + e);
        }
    }
}
