package fi.poltsi.vempain.admin.security.jwt;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
class AuthEntryPointJwtUTC {

    private AuthEntryPointJwt authEntryPointJwt;

    @BeforeEach
    void setUp() {
        authEntryPointJwt = new AuthEntryPointJwt();
    }

    @Test
    void commenceOk() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            AuthenticationException ex = new BadCredentialsException("Test exception");
            authEntryPointJwt.commence(request, response, ex);
            assertNotNull(response);
            assertEquals("Error: Unauthorized", response.getErrorMessage());
        } catch (Exception e) {
            fail("Should not have received any exceptions when calling commence: " + e);
        }
    }
}
