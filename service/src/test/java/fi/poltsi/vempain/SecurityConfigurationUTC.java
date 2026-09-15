package fi.poltsi.vempain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigurationUTC {
	@Test
	void productionDefaultsDoNotEnableTestBypassOrUnrestrictedActuator() throws IOException {
		String configuration;
		try (InputStream stream = getClass().getResourceAsStream("/application.yaml")) {
			assertTrue(stream != null, "application.yaml must be available on the test classpath");
			configuration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}

		assertTrue(configuration.contains("default: prod"));
		assertTrue(configuration.contains("test: false"));
		assertTrue(configuration.contains("include: [health, info]"));
		assertTrue(configuration.contains("default: none"));
		assertFalse(configuration.contains("include: \"*\""));
		assertFalse(configuration.contains("password: vempain_"));
		assertFalse(configuration.contains("jwt-secret: override-me"));
	}
}
