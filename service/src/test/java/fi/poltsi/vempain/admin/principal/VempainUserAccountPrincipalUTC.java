package fi.poltsi.vempain.admin.principal;

import fi.poltsi.vempain.admin.tools.TestUTCTools;
import fi.poltsi.vempain.auth.entity.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class VempainUserAccountPrincipalUTC {
    VempainUserPrincipal vempainUserPrincipal;

    @BeforeEach
    void setUp() {
        UserAccount userAccount = TestUTCTools.generateUser(1L);
        userAccount.setPassword("SimplePassword");
        userAccount.setLoginName("erkki");
        vempainUserPrincipal = new VempainUserPrincipal(userAccount);
    }

    @Test
    void getAuthoritiesOk() {
        try {
            Collection<? extends GrantedAuthority> grantedAuthorities = vempainUserPrincipal.getAuthorities();
            assertNull(grantedAuthorities);
        } catch (Exception e) {
            fail("Should not have received a null value as response");
        }
    }

    @Test
    void getPasswordOk() {
        try {
            String password = vempainUserPrincipal.getPassword();
            assertEquals("SimplePassword", password);
        } catch (Exception e) {
            fail("Should not have received a null value as response");
        }
    }

    @Test
    void getUsernameOk() {
        try {
            String username = vempainUserPrincipal.getUsername();
            assertEquals("erkki", username);
        } catch (Exception e) {
            fail("Should not have received a null value as response");
        }
    }

    @Test
    void isAccountNonExpiredOk() {
        try {
            boolean isNonExpired = vempainUserPrincipal.isAccountNonExpired();
            assertTrue(isNonExpired);
        } catch (Exception e) {
            fail("Should not have received a null value as response");
        }
    }

    @Test
    void isAccountNonLockedOk() {
        try {
            boolean isNonExpired = vempainUserPrincipal.isAccountNonLocked();
            assertTrue(isNonExpired);
        } catch (Exception e) {
            fail("Should not have received a null value as response");
        }
    }

    @Test
    void isCredentialsNonExpiredOk() {
        try {
            boolean isCredentialNonExpired = vempainUserPrincipal.isCredentialsNonExpired();
            assertTrue(isCredentialNonExpired);
        } catch (Exception e) {
            fail("Should not have received a null value as response");
        }
    }

    @Test
    void isEnabledOk() {
        try {
            boolean isEnabled = vempainUserPrincipal.isEnabled();
            assertTrue(isEnabled);
        } catch (Exception e) {
            fail("Should not have received a null value as response");
        }
    }
}
