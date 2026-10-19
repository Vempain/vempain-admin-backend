package fi.poltsi.vempain.admin.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.poltsi.vempain.admin.metrics.AdminMetrics;
import fi.poltsi.vempain.admin.service.ApiTokenService;
import fi.poltsi.vempain.auth.service.UserDetailsServiceImpl;
import fi.poltsi.vempain.auth.service.UserService;

/**
 * Creates the API token filter as a bean for the security chain and keeps the servlet container from registering it a second time outside the chain.
 */
@Configuration
public class ApiTokenSecurityConfiguration {

    @Bean
    public ApiTokenAuthenticationFilter apiTokenAuthenticationFilter(ApiTokenService apiTokenService, UserService userService,
            UserDetailsServiceImpl userDetailsService, AdminMetrics adminMetrics) {
        return new ApiTokenAuthenticationFilter(apiTokenService, userService, userDetailsService, adminMetrics);
    }

    @Bean
    public FilterRegistrationBean<ApiTokenAuthenticationFilter> apiTokenFilterRegistration(ApiTokenAuthenticationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
