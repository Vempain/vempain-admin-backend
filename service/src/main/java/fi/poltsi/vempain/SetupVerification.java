package fi.poltsi.vempain;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
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
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@AllArgsConstructor
class SetupVerification implements ApplicationContextAware {
	private final String TYPE_STRING  = "string";
	private final String TYPE_NUMBER  = "number";
	private final String TYPE_PATH    = "path";
	private final String TYPE_FILE   = "file";

	private final String[][] requiredKeys = {{"vempain.admin.file.image-format", TYPE_STRING},
											 {"vempain.admin.file.thumbnail-size", TYPE_NUMBER},
											 {"vempain.admin.file.site-file-directory", TYPE_PATH},
											 {"vempain.admin.ssh.user", TYPE_STRING},
											 {"vempain.admin.ssh.home-dir", TYPE_PATH},
											 {"vempain.admin.ssh.private-key", TYPE_PATH},
											 {"vempain.cmd-line.exiftool", TYPE_FILE},
											 {"vempain.site.ssh.address", TYPE_STRING},
											 {"vempain.site.ssh.port", TYPE_NUMBER},
											 {"vempain.site.ssh.user", TYPE_STRING},
											 {"vempain.site.ssh.home-dir", TYPE_STRING}, // This needs to be a string because it is located on the remote
											 {"vempain.site.www-root", TYPE_STRING},
											 {"vempain.site.image-size", TYPE_NUMBER},
											 {"spring.admin-datasource.url", TYPE_STRING},
											 {"spring.admin-datasource.username", TYPE_STRING},
											 {"spring.site-datasource.url", TYPE_STRING},
											 {"spring.site-datasource.username", TYPE_STRING}};

	private ApplicationContext applicationContext;

	@EventListener
	public void checkEssentialConfigurations(ContextRefreshedEvent event) {
		final Environment env = event.getApplicationContext().getEnvironment();

		for (String[] keyPair : requiredKeys) {
			var value = env.getProperty(keyPair[0]);
			log.info("Verifying that key {} is defined and not empty: {}", keyPair[0], value);

			if (value == null || value.isEmpty()) {
				closeApplication("Missing configuration value for key: " + keyPair[0]);
			} else if (value.equals("override-me")) {
				closeApplication("Configuration value for key " + keyPair[0] + " is still set to default value");
			} else {
				var path = Paths.get(value);

				switch (keyPair[1]) {
					case TYPE_NUMBER:
						if (!NumberUtils.isCreatable(value)) {
							closeApplication("Failed to parse number from configuration " + keyPair[0] + " value " + value);
						}

						break;
					case TYPE_PATH:
						if (!Files.exists(path)) {
							closeApplication("Path from configuration " + keyPair[0] + " pointing to " + value + " does not exist");
						}
						break;
					case TYPE_FILE:
						if (!Files.exists(path)) {
							closeApplication("File from configuration " + keyPair[0] + " pointing to " + value + " does not exist");
						}

						if (!Files.isRegularFile(path)) {
							closeApplication("File from configuration " + keyPair[0] + " pointing to " + value + " is not a file");
						}

						if (!Files.isExecutable(path)) {
							closeApplication("File from configuration " + keyPair[0] + " pointing to " + value + " is not executable");
						}
						break;
					case TYPE_STRING:
						// For now we don't do any verification, might split this further to other types
						break;
					default:
						closeApplication("Unknown configuration type: " + keyPair[1]);
				}
			}
		}
	}

	private void closeApplication(String message) {
		log.error(message);
		log.error("Shutting down application");
		((ConfigurableApplicationContext) applicationContext).close();
	}

	@EventListener
	public void printAllConfiguration(ContextRefreshedEvent event) {
		final Environment env = event.getApplicationContext().getEnvironment();
		log.info("====== Environment and configuration ======");
		log.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
		final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
		StreamSupport.stream(sources.spliterator(), false)
					 .filter(ps -> ps instanceof EnumerablePropertySource)
					 .map(ps -> ((EnumerablePropertySource<?>) ps).getPropertyNames())
					 .flatMap(Arrays::stream)
					 .distinct()
					 .filter(prop -> !(prop.contains("credentials") || prop.contains("password")))
					 .forEach(prop -> printProperty(env, prop));
		log.info("===========================================");
	}


	private void printProperty(Environment env, String key) {
		try {
			log.info("{}: {}", key, env.getProperty(key));
		} catch (Exception e) {
			log.error("Failed to fetch property value for {}", key);
		}
	}

	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
