package fi.poltsi.vempain.admin.security;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.entity.ApiToken;
import fi.poltsi.vempain.admin.metrics.AdminMetrics;
import fi.poltsi.vempain.admin.service.ApiTokenService;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import fi.poltsi.vempain.auth.service.UserDetailsServiceImpl;
import fi.poltsi.vempain.auth.service.UserService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The token filter: a valid token on an allowed endpoint authenticates as the owner with the {@code API_TOKEN} authority, a missing or locked owner is rejected
 * like an invalid token, and the request path is compared without the context path and a trailing slash.
 */
@ExtendWith(MockitoExtension.class)
class ApiTokenAuthenticationFilterUTC {

    @Mock
    private ApiTokenService apiTokenService;
    @Mock
    private UserService userService;
    @Mock
    private UserDetailsServiceImpl userDetailsService;
    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private ApiTokenAuthenticationFilter filter() {
        return new ApiTokenAuthenticationFilter(apiTokenService, userService, userDetailsService, new AdminMetrics(registry));
    }

    private double counted(String result) {
        var counter = registry.find(AdminMetrics.API_TOKEN_REQUESTS)
            .tag("result", result)
            .counter();
        return counter == null ? 0 : counter.count();
    }

    private static MockHttpServletRequest request(String method, String uri) {
        var request = new MockHttpServletRequest(method, uri);
        request.setContextPath("/api");
        request.setRemoteAddr("10.0.9.5");
        request.addHeader(Constants.API_TOKEN_HEADER, "vat_secret");
        return request;
    }

    private static ApiToken token(long ownerId) {
        return ApiToken.builder()
            .id(1L)
            .tokenPrefix("vat_sec")
            .ownerUserId(ownerId)
            .expiresAt(Instant.now()
                .plusSeconds(3600))
            .network("10.0.9.0/24")
            .build();
    }

    @Test
    void withoutATokenHeaderTheRequestPassesThroughUntouched() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/content-management/file/site-file/users");
        var response = new MockHttpServletResponse();

        filter().doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(apiTokenService, never()).authenticate(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        assertNull(SecurityContextHolder.getContext()
            .getAuthentication());
    }

    @Test
    void anInvalidTokenIsUnauthorized() throws Exception {
        when(apiTokenService.authenticate("vat_secret", "10.0.9.5")).thenReturn(Optional.empty());
        var response = new MockHttpServletResponse();

        filter().doFilterInternal(request("GET", "/api/content-management/file/site-file/users"), response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void aTokenMayListTheGrantableUnits() throws Exception {
        when(apiTokenService.authenticate("vat_secret", "10.0.9.5")).thenReturn(Optional.of(token(5L)));
        when(userService.findById(5L)).thenReturn(Optional.of(UserAccount.builder()
            .id(5L)
            .loginName("file-backend")
            .locked(false)
            .build()));
        when(userDetailsService.loadUserByUsername("file-backend"))
            .thenReturn(new UserDetailsImpl(5L, "file-backend", "File backend", "file@nohost.nodomain", "secret", Set.of(), List.of()));
        var request = request("GET", "/api/content-management/file/site-file/units");
        var response = new MockHttpServletResponse();

        filter().doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext()
            .getAuthentication());
    }

    @Test
    void aTokenMayNotCallEndpointsOutsideTheAllowList() throws Exception {
        when(apiTokenService.authenticate("vat_secret", "10.0.9.5")).thenReturn(Optional.of(token(5L)));
        var response = new MockHttpServletResponse();

        filter().doFilterInternal(request("GET", "/api/admin-management/api-tokens"), response, filterChain);

        assertEquals(403, response.getStatus());
        verify(userService, never()).findById(org.mockito.ArgumentMatchers.anyLong());
        assertEquals(1, counted("forbidden_endpoint"), "the refusal is counted");
    }

    @Test
    void aTokenWhoseOwnerIsMissingIsUnauthorized() throws Exception {
        when(apiTokenService.authenticate("vat_secret", "10.0.9.5")).thenReturn(Optional.of(token(5L)));
        when(userService.findById(5L)).thenReturn(Optional.empty());
        var response = new MockHttpServletResponse();

        filter().doFilterInternal(request("GET", "/api/content-management/file/site-file/users"), response, filterChain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext()
            .getAuthentication());
        assertEquals(1, counted("missing_user"), "the refusal is counted");
    }

    @Test
    void aTokenWhoseOwnerIsLockedIsUnauthorized() throws Exception {
        when(apiTokenService.authenticate("vat_secret", "10.0.9.5")).thenReturn(Optional.of(token(5L)));
        when(userService.findById(5L)).thenReturn(Optional.of(UserAccount.builder()
            .id(5L)
            .loginName("file-backend")
            .locked(true)
            .build()));
        var response = new MockHttpServletResponse();

        filter().doFilterInternal(request("GET", "/api/content-management/file/site-file/users"), response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void aValidTokenAuthenticatesAsTheOwnerWithTheTokenAuthorityEvenWithATrailingSlash() throws Exception {
        when(apiTokenService.authenticate("vat_secret", "10.0.9.5")).thenReturn(Optional.of(token(5L)));
        when(userService.findById(5L)).thenReturn(Optional.of(UserAccount.builder()
            .id(5L)
            .loginName("file-backend")
            .locked(false)
            .build()));
        var principal = new UserDetailsImpl(5L, "file-backend", "File backend", "file@nohost.nodomain", "secret", Set.of(), List.of());
        when(userDetailsService.loadUserByUsername("file-backend")).thenReturn(principal);
        var request = request("GET", "/api/content-management/file/site-file/users/");
        var response = new MockHttpServletResponse();

        filter().doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var authentication = SecurityContextHolder.getContext()
            .getAuthentication();
        assertNotNull(authentication);
        assertEquals(principal, authentication.getPrincipal());
        assertTrue(authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(ApiTokenAuthenticationFilter.API_TOKEN_AUTHORITY::equals));
        assertNotNull(authentication.getDetails());
        assertEquals(1, counted("accepted"), "the accepted request is counted");
        assertEquals(0, counted("forbidden_endpoint"));
    }

    @Test
    void theAllowListCoversOnlyTheServiceEndpoints() {
        assertTrue(ApiTokenAuthenticationFilter.isAllowed("POST", "/content-management/file/site-file"));
        assertTrue(ApiTokenAuthenticationFilter.isAllowed("DELETE", "/content-management/file/site-file/12"));
        assertTrue(ApiTokenAuthenticationFilter.isAllowed("DELETE", "/content-management/data/gps_1"));
        assertFalse(ApiTokenAuthenticationFilter.isAllowed("GET", "/content-management/file/site-file"));
        assertFalse(ApiTokenAuthenticationFilter.isAllowed("GET", "/content-management/pages"));
    }
}
