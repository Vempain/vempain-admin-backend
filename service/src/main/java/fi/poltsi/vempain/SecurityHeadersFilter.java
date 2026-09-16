package fi.poltsi.vempain;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Applies response protections to API and management responses, including
 * responses produced before a controller is invoked.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
	private static final String CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'; object-src 'none'; base-uri 'none'; form-action 'none'";
	private static final String PERMISSIONS_POLICY      = "camera=(), microphone=(), geolocation=()";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("X-Frame-Options", "DENY");
		response.setHeader("Referrer-Policy", "no-referrer");
		response.setHeader("Permissions-Policy", PERMISSIONS_POLICY);
		response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
		response.setHeader("Cache-Control", "no-store");
		filterChain.doFilter(request, response);
	}
}
