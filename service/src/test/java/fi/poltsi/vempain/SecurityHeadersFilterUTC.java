package fi.poltsi.vempain;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SecurityHeadersFilterUTC {
	@Test
	void appliesSecurityHeadersBeforeDelegating() throws Exception {
		var response = new MockHttpServletResponse();
		var chain = mock(FilterChain.class);

		new SecurityHeadersFilter().doFilter(new MockHttpServletRequest(), response, chain);

		assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
		assertEquals("DENY", response.getHeader("X-Frame-Options"));
		assertEquals("no-store", response.getHeader("Cache-Control"));
		assertEquals("max-age=31536000; includeSubDomains", response.getHeader("Strict-Transport-Security"));
	}
}
