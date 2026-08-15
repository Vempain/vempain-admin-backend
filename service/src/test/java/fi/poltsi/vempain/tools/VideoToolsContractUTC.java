package fi.poltsi.vempain.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VideoToolsContractUTC {
	@Test
	void invalidImageSizeFallsBackToZeroDimensions() {
		assertEquals(0, VideoTools.getVideoDimensions(new org.json.JSONObject("""
																					  {"File": {"ImageSize": "invalid"}}
																					  """)).width);
	}
}
