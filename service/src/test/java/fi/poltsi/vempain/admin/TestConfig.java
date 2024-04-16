package fi.poltsi.vempain.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.context.annotation.Bean;

@AutoConfigureJsonTesters
public class TestConfig {

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		// Register the JavaTimeModule to handle Java 8 date/time types
		objectMapper.registerModule(new JavaTimeModule());
		return objectMapper;
	}
}
