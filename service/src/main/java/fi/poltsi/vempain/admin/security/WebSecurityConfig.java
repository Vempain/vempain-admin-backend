package fi.poltsi.vempain.admin.security;

import fi.poltsi.vempain.auth.security.jwt.AuthEntryPointJwt;
import fi.poltsi.vempain.auth.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import static fi.poltsi.vempain.admin.api.Constants.REST_ADMIN_PREFIX;
import static fi.poltsi.vempain.admin.api.Constants.REST_CONTENT_PREFIX;
import static fi.poltsi.vempain.admin.api.Constants.REST_FILE_PREFIX;
import static fi.poltsi.vempain.admin.api.Constants.REST_SCHEDULE_PREFIX;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig extends fi.poltsi.vempain.auth.security.WebSecurityConfig {

	public WebSecurityConfig(UserDetailsServiceImpl userDetailsServiceImpl, AuthEntryPointJwt authEntryPointJwt, Environment environment) {
		super(userDetailsServiceImpl, authEntryPointJwt, environment);
	}

	@Override
	protected void configureApplicationAuthorization(ApplicationAuthorizationConfigurer authorization) {
		// /tasks is the progress API of the shared background task facility (fi.poltsi.vempain.common.task)
		authorization.authenticated(REST_CONTENT_PREFIX + "/**", REST_FILE_PREFIX + "/**",
									REST_SCHEDULE_PREFIX + "/**", REST_ADMIN_PREFIX + "/**", "/tasks/**");
	}
}
