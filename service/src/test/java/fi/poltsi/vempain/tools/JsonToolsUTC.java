package fi.poltsi.vempain.tools;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonToolsUTC {
	@Test
	void extractsMetadataFromSupportedLocations() {
		var json = new JSONObject("""
										  {
										    "XMP": {"Description": "description", "Subject": ["one", "two"], "MIMEType": "image/jpeg"},
										    "ExifIFD": {"DateTimeOriginal": "2026:08:15 12:00:00", "SubSecTimeOriginal": "123"},
										    "XMP-xmpMM": {"DocumentID": "doc-1"}
										  }
										  """);

		assertTrue(JsonTools.getJSONObject(json, "XMP")
		                    .isPresent());
		assertFalse(JsonTools.getJSONObject(json, "Missing")
		                     .isPresent());
		assertEquals("description", JsonTools.getDescriptionFromJson(json));
		assertEquals("image/jpeg", JsonTools.extractMimetype(json));
		assertEquals("2026:08:15 12:00:00", JsonTools.getOriginalDateTimeFromJson(json));
		assertEquals(123, JsonTools.getOriginalSecondFraction(json));
		assertEquals("doc-1", JsonTools.getOriginalDocumentId(json));
		assertEquals(List.of("one", "two"), JsonTools.getSubjects(json));
	}

	@Test
	void handlesScalarAndMalformedMetadataValues() {
		var json = new JSONObject("""
										  {
										    "XMP": {"Subject": "single"},
										    "ExifIFD": {"SubSecTimeOriginal": "456"},
										    "IPTC": {"Keywords": ["keyword"] }
										  }
										  """);
		assertEquals(456, JsonTools.getOriginalSecondFraction(json));
		assertEquals(List.of("single"), JsonTools.getSubjects(new JSONObject("""
																					 {"XMP": {"Subject": "single"}}
																					 """)));
		assertEquals(List.of("keyword"), JsonTools.extractJsonArray(json, Map.of("IPTC", List.of("Keywords"))));
		assertEquals(List.of(), JsonTools.extractJsonArray(new JSONObject(), Map.of("IPTC", List.of("Keywords"))));
	}
}
