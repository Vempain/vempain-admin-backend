package fi.poltsi.vempain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;

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

	@TempDir
	Path tempDir;

	private Map<String, String> validConfiguration() throws Exception {
		var exiftool = tempDir.resolve("exiftool");
		Files.writeString(exiftool, "#!/bin/sh\nexit 0\n");
		Files.setPosixFilePermissions(exiftool, PosixFilePermissions.fromString("rwxr-xr-x"));
		var configuration = new HashMap<String, String>();
		configuration.put("vempain.app.frontend-url", "https://admin.example");
		configuration.put("vempain.app.jwt-secret", "secret");
		configuration.put("vempain.admin.file.site-file-directory", tempDir.toString());
		configuration.put("vempain.admin.ssh.user", "vempain-admin");
		configuration.put("vempain.admin.ssh.home-dir", tempDir.toString());
		configuration.put("vempain.site.www-root", "/var/www");
		configuration.put("vempain.site.ssh.user", "vempain-site");
		configuration.put("vempain.site.ssh.home-dir", "/home/vempain-site");
		configuration.put("vempain.site.ssh.address", "site.example");
		configuration.put("vempain.cmd-line.exiftool", exiftool.toString());
		configuration.put("vempain.app.jwt-expiration-ms", "86400000");
		configuration.put("vempain.admin.file.thumbnail-size", "250");
		configuration.put("vempain.site.image-size", "800");
		configuration.put("vempain.site.ssh.port", "22");
		configuration.put("vempain.cors.max-age", "3600");
		configuration.put("spring.admin-datasource.url", TLS_ADMIN_URL);
		configuration.put("spring.admin-datasource.username", "admin");
		configuration.put("spring.site-datasource.url", TLS_SITE_URL);
		configuration.put("spring.site-datasource.username", "site");
		return configuration;
	}

	private static ConfigurableApplicationContext contextWith(Map<String, String> configuration) {
		var environment = new MockEnvironment();
		configuration.forEach(environment::setProperty);
		var context = mock(ConfigurableApplicationContext.class);
		when(context.getEnvironment()).thenReturn(environment);
		return context;
	}

	private void assertClosesWith(Map<String, String> configuration) {
		var context = contextWith(configuration);

		new SetupVerification(context).checkEssentialConfigurations(new ContextRefreshedEvent(context));

		// Every failed check of the same key asks the context to close; the mock does not stop the loop
		verify(context, org.mockito.Mockito.atLeastOnce()).close();
	}

	@Test
	void checkEssentialConfigurations_acceptsACompleteConfiguration() throws Exception {
		var context = contextWith(validConfiguration());

		new SetupVerification(context).checkEssentialConfigurations(new ContextRefreshedEvent(context));

		verify(context, never()).close();
	}

	@Test
	void checkEssentialConfigurations_closesOnAMissingOrDefaultValue() throws Exception {
		var missing = validConfiguration();
		missing.remove("vempain.app.jwt-secret");
		assertClosesWith(missing);

		var empty = validConfiguration();
		empty.put("vempain.site.ssh.address", "");
		assertClosesWith(empty);

		var unchanged = validConfiguration();
		unchanged.put("vempain.app.frontend-url", "override-me");
		assertClosesWith(unchanged);
	}

	@Test
	void checkEssentialConfigurations_closesOnANumberThatDoesNotParse() throws Exception {
		var configuration = validConfiguration();
		configuration.put("vempain.site.ssh.port", "twenty-two");
		assertClosesWith(configuration);
	}

	@Test
	void checkEssentialConfigurations_closesOnAMissingPath() throws Exception {
		var configuration = validConfiguration();
		configuration.put("vempain.admin.ssh.home-dir", tempDir.resolve("missing")
															   .toString());
		assertClosesWith(configuration);
	}

	@Test
	void checkEssentialConfigurations_closesWhenTheToolIsMissingADirectoryOrNotExecutable() throws Exception {
		var missing = validConfiguration();
		missing.put("vempain.cmd-line.exiftool", tempDir.resolve("no-such-tool")
														.toString());
		assertClosesWith(missing);

		var directory = validConfiguration();
		directory.put("vempain.cmd-line.exiftool", tempDir.toString());
		assertClosesWith(directory);

		var notExecutable = validConfiguration();
		var script = tempDir.resolve("not-executable");
		Files.writeString(script, "#!/bin/sh\n");
		Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rw-r--r--"));
		notExecutable.put("vempain.cmd-line.exiftool", script.toString());
		assertClosesWith(notExecutable);
	}

	@Test
	void printAllConfiguration_hidesSensitiveKeysAndSurvivesUnreadableValues() {
		var environment = new MockEnvironment().withProperty("vempain.app.jwt-secret", "hidden")
											   .withProperty("vempain.site.ssh.address", "site.example");
		var context = mock(ConfigurableApplicationContext.class);
		when(context.getEnvironment()).thenReturn(environment);

		new SetupVerification(context).printAllConfiguration(new ContextRefreshedEvent(context));

		verify(context, never()).close();
	}
}
