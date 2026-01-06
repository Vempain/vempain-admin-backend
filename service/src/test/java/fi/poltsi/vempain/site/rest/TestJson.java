package fi.poltsi.vempain.site.rest;

import lombok.SneakyThrows;
import tools.jackson.databind.ObjectMapper;

public final class TestJson {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TestJson() {
	}

	@SneakyThrows
	public static <T> T read(String json, Class<T> clazz) {
		return MAPPER.readValue(json, clazz);
	}
}

