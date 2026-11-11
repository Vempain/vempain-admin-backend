package fi.poltsi.vempain;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.math.NumberUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
class SetupVerification implements ApplicationContextAware {

    private final String typeString = "string";
    private final String typeNumber = "number";
    private final String typePath = "path";
    private final String typeFile = "file";

    private final String[][] requiredKeys = {
        { "vempain.app.frontend-url", typeString },
        { "vempain.app.jwt-secret", typeString },
        { "vempain.admin.file.site-file-directory", typePath },
        { "vempain.admin.ssh.user", typeString },
        { "vempain.admin.ssh.home-dir", typePath },
        { "vempain.site.www-root", typeString },
        { "vempain.site.ssh.user", typeString },
        { "vempain.site.ssh.home-dir", typeString }, // This needs to be a string because it is located on the remote
        { "vempain.site.ssh.address", typeString },
        { "vempain.exiftool.path", typeFile },
        // Following are already defined with default values, but we want to make sure they're of the correct type
        { "vempain.app.jwt-expiration-ms", typeNumber },
        { "vempain.admin.file.thumbnail-size", typeNumber },
        { "vempain.site.image-size", typeNumber },
        { "vempain.site.ssh.port", typeNumber },
        { "vempain.cors.max-age", typeNumber },
        { "spring.admin-datasource.url", typeString },
        { "spring.admin-datasource.username", typeString },
        { "spring.site-datasource.url", typeString },
        { "spring.site-datasource.username", typeString } };

    private static final Set<String> TLS_SSL_MODES = Set.of("require", "verify-ca", "verify-full");
    private static final List<String> PRODUCTION_DATASOURCE_URL_KEYS = List.of("spring.admin-datasource.url", "spring.site-datasource.url");

    private ApplicationContext applicationContext;

    @EventListener
    public void checkEssentialConfigurations(ContextRefreshedEvent event) {
        final Environment env = event.getApplicationContext()
            .getEnvironment();

        for (String[] keyPair : requiredKeys) {
            var value = env.getProperty(keyPair[0]);
            log.debug("Verifying that key {} is defined and not empty", keyPair[0]);

            if (value == null
                || value.isEmpty()) {
                closeApplication("Missing configuration value for key: " + keyPair[0]);
            } else if (value.equals("override-me")) {
                closeApplication("Configuration value for key " + keyPair[0] + " is still set to default value");
            } else {
                switch (keyPair[1]) {
                    case typeNumber:
                        if (!NumberUtils.isCreatable(value)) {
                            closeApplication("Failed to parse number from configuration " + keyPair[0] + " value " + value);
                        }

                        break;
                    case typePath:
                        var path = Paths.get(value);
                        if (!Files.exists(path)) {
                            closeApplication("Path from configuration " + keyPair[0] + " pointing to " + value + " does not exist");
                        }
                        break;
                    case typeFile:
                        var file = Paths.get(value);
                        if (!Files.exists(file)) {
                            closeApplication("File from configuration " + keyPair[0] + " pointing to " + value + " does not exist");
                        }

                        if (!Files.isRegularFile(file)) {
                            closeApplication("File from configuration " + keyPair[0] + " pointing to " + value + " is not a file");
                        }

                        if (!Files.isExecutable(file)) {
                            closeApplication("File from configuration " + keyPair[0] + " pointing to " + value + " is not executable");
                        }
                        break;
                    case typeString:
                        // For now we don't do any verification, might split this further to other types
                        break;
                    default :
                        closeApplication("Unknown configuration type: " + keyPair[1]);
                }
            }
        }
    }

    @EventListener
    public void verifyProductionDatasources(ContextRefreshedEvent event) {
        final Environment env = event.getApplicationContext()
            .getEnvironment();
        if (!env.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }

        for (var key : PRODUCTION_DATASOURCE_URL_KEYS) {
            if (!requiresTls(env.getProperty(key, ""))) {
                closeApplication("Production datasource " + key + " must require TLS: set sslmode=require, verify-ca or verify-full");
                return;
            }
        }
    }

    /**
     * Tells whether a PostgreSQL JDBC URL makes the driver insist on TLS: {@code sslmode} is require, verify-ca or verify-full, or {@code ssl} is given without
     * an {@code sslmode} (the driver then verifies the full certificate). Weaker modes such as prefer or allow fall back to plaintext when the server offers no
     * TLS, so they do not count.
     */
    static boolean requiresTls(String jdbcUrl) {
        var queryStart = jdbcUrl == null ? -1 : jdbcUrl.indexOf('?');
        if (queryStart < 0) {
            return false;
        }

        String sslMode = null;
        var ssl = false;
        for (var parameter : jdbcUrl.substring(queryStart + 1)
            .split("&")) {
            var separator = parameter.indexOf('=');
            var name = (separator < 0 ? parameter : parameter.substring(0, separator)).trim()
                .toLowerCase(Locale.ROOT);
            var value = separator < 0 ? ""
                    : URLDecoder.decode(parameter.substring(separator + 1), StandardCharsets.UTF_8)
                        .trim()
                        .toLowerCase(Locale.ROOT);
            if ("sslmode".equals(name)) {
                sslMode = value;
            } else if ("ssl".equals(name)) {
                ssl = !"false".equals(value);
            }
        }

        return sslMode == null ? ssl : TLS_SSL_MODES.contains(sslMode);
    }

    private void closeApplication(String message) {
        log.error(message);
        log.error("Shutting down application");
        ((ConfigurableApplicationContext) applicationContext).close();
    }

    @EventListener
    public void printAllConfiguration(ContextRefreshedEvent event) {
        final Environment env = event.getApplicationContext()
            .getEnvironment();
        log.debug("====== Environment and configuration ======");
        log.debug("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
        final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
        StreamSupport.stream(sources.spliterator(), false)
            .filter(ps -> ps instanceof EnumerablePropertySource)
            .map(ps -> ((EnumerablePropertySource<?>) ps).getPropertyNames())
            .flatMap(Arrays::stream)
            .distinct()
            .filter(prop -> !isSensitiveProperty(prop))
            .sorted()
            .forEach(prop -> printProperty(env, prop));
        log.debug("===========================================");
    }

    private void printProperty(Environment env, String key) {
        try {
            log.debug("{}: {}", key, env.getProperty(key));
        } catch (Exception e) {
            log.error("Failed to fetch property value for {}", key);
        }
    }

    private boolean isSensitiveProperty(String key) {
        var normalized = key.toLowerCase();
        return normalized.contains("password")
               || normalized.contains("secret")
               || normalized.contains("token")
               || normalized.contains("private-key")
               || normalized.contains("credentials")
               || normalized.contains("api-key");
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
