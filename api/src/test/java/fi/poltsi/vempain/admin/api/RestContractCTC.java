package fi.poltsi.vempain.admin.api;

import fi.poltsi.vempain.admin.api.response.LayoutResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestContractCTC {
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void publicEnumsAndPathsRemainStable() {
		assertEquals("PAGE", ContentTypeEnum.PAGE.name());
		assertEquals("GALLERY", ContentTypeEnum.GALLERY.name());
		assertEquals("OK", PublishResultEnum.OK.name());
		assertEquals("FAIL", PublishResultEnum.FAIL.name());
		assertEquals("FULL", QueryDetailEnum.FULL.name());
		assertEquals("/content-management", Constants.REST_CONTENT_PREFIX);
		assertEquals("/content-management/file", Constants.REST_FILE_PREFIX);
		assertEquals("/content-management/data", Constants.REST_DATA_PREFIX);
	}

	@Test
	void layoutResponseUsesThePublishedSnakeCaseContract() throws Exception {
		String json = """
				{
				  "id": 7,
				  "layout_name": "Landing page",
				  "structure": "<!--comp_1-->",
				  "locked": 0,
				  "creator": 1,
				  "created": "2026-08-15T12:00:00Z",
				  "modifier": 1,
				  "modified": "2026-08-15T12:00:00Z"
				}
				""";

		LayoutResponse response = objectMapper.readValue(json, LayoutResponse.class);
		String serialized = objectMapper.writeValueAsString(response);

		assertEquals("Landing page", response.getLayoutName());
		assertTrue(serialized.contains("\"layout_name\""));
		assertFalse(serialized.contains("\"layoutName\""));
	}
}
