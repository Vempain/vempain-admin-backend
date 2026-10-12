package fi.poltsi.vempain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SetupVerificationUTC {

	private static final String TLS_ADMIN_URL = "jdbc:postgresql://admin-db:5432/vempain_admin?currentSchema=vempain_admin&sslmode=require";
	private static final String TLS_SITE_URL  = "jdbc:postgresql://site-db:5432/vempain_site?currentSchema=vempain_site&sslmode=verify-full";

	@ParameterizedTest
	@ValueSource(strings = {
			"jdbc:postgresql://db:5432/vempain?sslmode=require",
			"jdbc:postgresql://db:5432/vempain?currentSchema=vempain&sslmode=verify-ca",
			"jdbc:postgresql://db:5432/vempain?sslmode=VERIFY-FULL&currentSchema=vempain",
			"jdbc:postgresql://db:5432/vempain?ssl=true",
			"jdbc:postgresql://db:5432/vempain?ssl"
	})
	void requiresTls_acceptsUrlsThatInsistOnTls(String url) {
		assertTrue(SetupVerification.requiresTls(url));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"jdbc:postgresql://db:5432/vempain",
			"jdbc:postgresql://db:5432/vempain?currentSchema=vempain",
			"jdbc:postgresql://db:5432/vempain?sslmode=prefer",
			"jdbc:postgresql://db:5432/vempain?sslmode=allow",
			"jdbc:postgresql://db:5432/vempain?sslmode=disable",
			"jdbc:postgresql://db:5432/vempain?ssl=false",
			"jdbc:postgresql://db:5432/vempain?useSSL=true",
			"jdbc:postgresql://db:5432/vempain?sslmode=disable&ssl=true"
	})
	void requiresTls_rejectsUrlsThatAllowPlaintext(String url) {
		assertFalse(SetupVerification.requiresTls(url));
	}

	@Test
	void verifyProductionDatasources_acceptsTlsOnBothDatasourcesInProd() {
		var context = contextWith("prod", TLS_ADMIN_URL, TLS_SITE_URL);

		new SetupVerification(context).verifyProductionDatasources(new ContextRefreshedEvent(context));

		verify(context, never()).close();
	}

	@Test
	void verifyProductionDatasources_closesApplicationWhenTheAdminDatasourceAllowsPlaintext() {
		var context = contextWith("prod", "jdbc:postgresql://admin-db:5432/vempain_admin?currentSchema=vempain_admin", TLS_SITE_URL);

		new SetupVerification(context).verifyProductionDatasources(new ContextRefreshedEvent(context));

		verify(context).close();
	}

	@Test
	void verifyProductionDatasources_closesApplicationWhenTheSiteDatasourceAllowsPlaintext() {
		var context = contextWith("prod", TLS_ADMIN_URL, "jdbc:postgresql://site-db:5432/vempain_site?sslmode=disable");

		new SetupVerification(context).verifyProductionDatasources(new ContextRefreshedEvent(context));

		verify(context).close();
	}

	@Test
	void verifyProductionDatasources_ignoresOtherProfiles() {
		var context = contextWith("test", "jdbc:tc:postgresql:18-alpine:///vempain_admin", "jdbc:tc:postgresql:18-alpine:///vempain_site");

		new SetupVerification(context).verifyProductionDatasources(new ContextRefreshedEvent(context));

		verify(context, never()).close();
	}

	private static ConfigurableApplicationContext contextWith(String profile, String adminUrl, String siteUrl) {
		var environment = new MockEnvironment().withProperty("spring.admin-datasource.url", adminUrl)
											   .withProperty("spring.site-datasource.url", siteUrl);
		environment.setActiveProfiles(profile);
		var context = mock(ConfigurableApplicationContext.class);
		when(context.getEnvironment()).thenReturn(environment);
		return context;
	}
}
