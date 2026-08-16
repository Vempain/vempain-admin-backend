package fi.poltsi.vempain.tools;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VideoToolsUTC {
	@Test
	void convertsSupportedDurations() {
		assertEquals(0L, VideoTools.getVideoLength(null));
		assertEquals(3723L, VideoTools.detectAndConvertTime("1:02:03"));
		assertEquals(12L, VideoTools.detectAndConvertTime("12s"));
		assertEquals(12L, VideoTools.detectAndConvertTime("12"));
		assertEquals(0L, VideoTools.convertHMSStringToSeconds(null));
		assertEquals(0L, VideoTools.convertHMSStringToSeconds("12:34"));

		assertEquals(12L, VideoTools.getVideoLength(new JSONObject("""
																		   {"Track1": {"TrackDuration": "12s"}}
																		   """)));
		assertEquals(13L, VideoTools.getVideoLength(new JSONObject("""
																		   {"Track2": {"MediaDuration": "13"}}
																		   """)));
		assertEquals(14L, VideoTools.getVideoLength(new JSONObject("""
																		   {"Composite": {"Duration": "14s"}}
																		   """)));
	}

	@Test
	void resolvesVideoDimensionsFromMetadataFormats() {
		assertEquals(new Dimension(1920, 1080), VideoTools.getVideoDimensions(new JSONObject("""
																									 {"File": {"ImageWidth": 1920, "ImageHeight": 1080}}
																									 """)));
		assertEquals(new Dimension(800, 600), VideoTools.getVideoDimensions(new JSONObject("""
																								   {"Track1": {"SourceImageWidth": 800, "SourceImageHeight": 600}}
																								   """)));
		assertEquals(new Dimension(640, 480), VideoTools.getVideoDimensions(new JSONObject("""
																								   {"Composite": {"ImageSize": "640x480"}}
																								   """)));
		assertEquals(new Dimension(0, 0), VideoTools.getVideoDimensions(null));
		assertEquals(new Dimension(0, 0), VideoTools.getVideoDimensions(new JSONObject()));
	}
}
