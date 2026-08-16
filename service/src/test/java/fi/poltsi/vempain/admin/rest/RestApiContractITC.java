package fi.poltsi.vempain.admin.rest;

import fi.poltsi.vempain.admin.AbstractITCTest;
import fi.poltsi.vempain.admin.api.request.file.FileIngestRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestApiContractITC extends AbstractITCTest {
	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void fileIngestJsonContractRemainsSnakeCase() throws Exception {
		String json = """
				{
				  "file_name": "image.jpg",
				  "file_path": "gallery/summer",
				  "mime_type": "image/jpeg",
				  "comment": "A photograph",
				  "metadata": "{}",
				  "sha256sum": "abc123",
				  "gallery_id": 42,
				  "gallery_name": "Summer",
				  "gallery_description": "Summer photographs"
				}
				""";

		FileIngestRequest request = objectMapper.readValue(json, FileIngestRequest.class);
		String serialized = objectMapper.writeValueAsString(request);

		assertEquals("image.jpg", request.getFileName());
		assertEquals(42L, request.getGalleryId());
		assertTrue(serialized.contains("\"file_name\""));
		assertTrue(serialized.contains("\"gallery_id\""));
		assertFalse(serialized.contains("\"fileName\""));
		assertFalse(serialized.contains("\"galleryId\""));
	}
}
