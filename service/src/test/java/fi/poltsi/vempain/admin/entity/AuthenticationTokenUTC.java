package fi.poltsi.vempain.admin.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationTokenUTC {
    @Test
    void createAuthenticationToken() {
        AuthenticationToken authenticationToken = new AuthenticationToken();
        assertNull(authenticationToken.authToken);
        assertTrue(Instant.now().plus(2, ChronoUnit.MILLIS).isAfter(authenticationToken.expiration.toInstant(ZoneOffset.UTC)));
    }
}
