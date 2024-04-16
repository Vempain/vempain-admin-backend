package fi.poltsi.vempain.admin.security.jwt;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties()
@TestPropertySource(properties = {"vempain.app.jwtSecret=OUVENkFDQjk2REI2RTRFREE0QkQzREQ5RDRCRjhFM0YxNDA3MUJBQjVCNDExNEJBM0FFOEExMjI5MEUxRTQ5RAo=",
                                  "vempain.app.jwtExpirationMs=86400000"})
class JwtUtilsUTC {
	@Autowired
    private JwtUtils                   jwtUtils;
    private InMemoryUserDetailsManager inMemoryUserDetailsManager;

    @Autowired
    AuthenticationManager authenticationManager;

    @Value("${vempain.app.jwtSecret}")
    private String jwtSecret;

    @Value("${vempain.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @BeforeEach
    void setUp() {
        PasswordEncoder   passwordEncoder = new BCryptPasswordEncoder();
        List<UserDetails> userDetailsList = new ArrayList<>();
        userDetailsList.add(User.withUsername("employee").password(passwordEncoder.encode("password")).roles("EMPLOYEE", "USER").build());
        userDetailsList.add(User.withUsername("manager").password(passwordEncoder.encode("password")).roles("MANAGER", "USER").build());
        inMemoryUserDetailsManager = new InMemoryUserDetailsManager(userDetailsList);
    }

    @Test
    void validateJwtTokenOk() {
        var jws = jwtUtils.generateJwtTokenForUser("An Employee", "employee", "password");
        try {
            assertTrue(jwtUtils.validateJwtToken(jws));
        } catch (Exception e) {
            fail("Should not have caught an exception: " + e);
        }
    }

    @Test
    void invalidJwtTokenFail() {
        try {
            assertFalse(jwtUtils.validateJwtToken("Didi"));
        } catch (Exception e) {
            fail("Should not have caught an exception: " + e);
        }
    }

    @Test
    void validateJwtTokenJWTClaimsEmptyFail() {
        try {
            assertFalse(jwtUtils.validateJwtToken("Didi"));
        } catch (Exception e) {
            fail("Should not have caught an exception: " + e);
        }
    }

    @Test
    void getPropertyValuesOk() {
        assertNotNull(jwtSecret);
        assertEquals("OUVENkFDQjk2REI2RTRFREE0QkQzREQ5RDRCRjhFM0YxNDA3MUJBQjVCNDExNEJBM0FFOEExMjI5MEUxRTQ5RAo=", jwtSecret);
        log.info("Secret: {}", jwtSecret);
        assertEquals(86400000, jwtExpirationMs);
        log.info("Expiration: {}", jwtExpirationMs);
    }
}
