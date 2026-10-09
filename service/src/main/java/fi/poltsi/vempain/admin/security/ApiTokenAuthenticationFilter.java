package fi.poltsi.vempain.admin.security;

import fi.poltsi.vempain.admin.api.Constants;
import fi.poltsi.vempain.admin.service.ApiTokenService;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import fi.poltsi.vempain.auth.service.UserDetailsServiceImpl;
import fi.poltsi.vempain.auth.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Authenticates service-to-service requests that carry an API token in {@link Constants#API_TOKEN_HEADER}. A valid token authenticates
 * the request as the token's owner, but only for the endpoints the file backend needs ({@link #ALLOWED}); any other path answers 403
 * so that a leaked token can not administer the service, and an invalid, expired or out-of-network token answers 401 without ever
 * falling back to another credential.
 */
@Slf4j
@RequiredArgsConstructor
public class ApiTokenAuthenticationFilter extends OncePerRequestFilter {
	/**
	 * Authority marking a token authenticated request, for logging and tests.
	 */
	public static final String API_TOKEN_AUTHORITY = "API_TOKEN";

	/**
	 * Method + path (relative to the context path) patterns a token may call: file ingest, undo of an ingest, the grantable users, the
	 * paged site file listing used by the refresh schedule and data set management. Never the token management or anything else.
	 */
	static final List<AllowedCall> ALLOWED = List.of(
			new AllowedCall(HttpMethod.POST, Constants.REST_FILE_PREFIX + "/site-file"),
			new AllowedCall(HttpMethod.DELETE, Constants.REST_FILE_PREFIX + "/site-file/*"),
			new AllowedCall(HttpMethod.GET, Constants.REST_FILE_PREFIX + "/site-file/users"),
			new AllowedCall(HttpMethod.POST, Constants.REST_FILE_PREFIX + "/site-files/paged"),
			new AllowedCall(HttpMethod.GET, Constants.REST_DATA_PREFIX + "/*"),
			new AllowedCall(HttpMethod.POST, Constants.REST_DATA_PREFIX),
			new AllowedCall(HttpMethod.PUT, Constants.REST_DATA_PREFIX),
			new AllowedCall(HttpMethod.DELETE, Constants.REST_DATA_PREFIX + "/*"));

	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

	private final ApiTokenService        apiTokenService;
	private final UserService            userService;
	private final UserDetailsServiceImpl userDetailsService;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
			throws ServletException, IOException {
		var presented = request.getHeader(Constants.API_TOKEN_HEADER);

		if (presented == null || presented.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}

		var path = requestPath(request);
		var token = apiTokenService.authenticate(presented, request.getRemoteAddr());

		if (token.isEmpty()) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API token");
			return;
		}

		if (!isAllowed(request.getMethod(), path)) {
			log.warn("API token {} used on {} {}, which tokens may not call", token.get()
																				   .getTokenPrefix(), request.getMethod(), path);
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "API tokens may not call this endpoint");
			return;
		}

		var owner = userService.findById(token.get()
											  .getOwnerUserId());
		if (owner.isEmpty() || owner.get()
									.isLocked()) {
			log.warn("API token {} belongs to a missing or locked user {}", token.get()
																				 .getTokenPrefix(), token.get()
																										 .getOwnerUserId());
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API token");
			return;
		}

		var principal = (UserDetailsImpl) userDetailsService.loadUserByUsername(owner.get()
																					 .getLoginName());
		var authorities = new ArrayList<GrantedAuthority>(principal.getAuthorities());
		authorities.add(new SimpleGrantedAuthority(API_TOKEN_AUTHORITY));
		var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext()
							 .setAuthentication(authentication);
		filterChain.doFilter(request, response);
	}

	public static boolean isAllowed(String method, String path) {
		return ALLOWED.stream()
					  .anyMatch(call -> call.method()
											.matches(method) && PATH_MATCHER.match(call.pattern(), path));
	}

	private static String requestPath(HttpServletRequest request) {
		var uri = request.getRequestURI();
		var context = request.getContextPath();
		var path = context != null && !context.isEmpty() && uri.startsWith(context) ? uri.substring(context.length()) : uri;
		return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
	}

	record AllowedCall(HttpMethod method, String pattern) {
	}
}
