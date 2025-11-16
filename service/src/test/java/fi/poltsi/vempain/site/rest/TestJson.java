package fi.poltsi.vempain.site.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public final class TestJson {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TestJson() {
	}

	@SneakyThrows
	public static <T> T read(String json, Class<T> clazz) {
		return MAPPER.readValue(json, clazz);
	}
}

