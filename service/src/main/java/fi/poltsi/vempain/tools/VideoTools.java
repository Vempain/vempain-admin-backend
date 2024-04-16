package fi.poltsi.vempain.tools;

import org.json.JSONObject;

import java.awt.*;
import java.util.List;

public class VideoTools {
	public static long getVideoLength(JSONObject jsonObject) {
		if (jsonObject == null) {
			return 0L;
		}

		// If jsonObject has a TrackX section, use that
		// Create a list of track names

		for (String trackName : List.of("Track1", "Track2", "Track3")) {
			if (jsonObject.has(trackName)) {
				JSONObject track1 = jsonObject.getJSONObject(trackName);

				if (track1.has("TrackDuration")) {
					return detectAndConvertTime(track1.getString("TrackDuration"));
				} else if (track1.has("MediaDuration")) {
					return detectAndConvertTime(track1.getString("MediaDuration"));
				}
			}
		}

		if (jsonObject.has("QuickTime")) {
			JSONObject quickTime = jsonObject.getJSONObject("Track1");

			if (quickTime.has("Duration")) {
				return detectAndConvertTime(quickTime.getString("Duration"));
			}
		}

		if (jsonObject.has("Composite")) {
			JSONObject composite = jsonObject.getJSONObject("Composite");

			if (composite.has("Duration")) {
				return detectAndConvertTime(composite.getString("Duration"));
			}
		}

		return 0L;
	}

	public static Dimension getVideoDimensions(JSONObject jsonObject) {
		if (jsonObject == null) {
			return new Dimension(0, 0);
		}

		for (String trackName : List.of("Composite", "File", "RIFF","Track1", "Track2", "Track3")) {
			if (jsonObject.has(trackName)) {
				JSONObject metadataSection = jsonObject.getJSONObject(trackName);

				if (metadataSection.has("ImageWidth") && metadataSection.has("ImageHeight")) {
					return new Dimension(metadataSection.getInt("ImageWidth"), metadataSection.getInt("ImageHeight"));
				} else if (metadataSection.has("SourceImageWidth") && metadataSection.has("SourceImageHeight")) {
					return new Dimension(metadataSection.getInt("SourceImageWidth"), metadataSection.getInt("SourceImageHeight"));
				} else if (metadataSection.has("ImageSize")) {
					return getDimensionsFromImageSize(metadataSection.getString("ImageSize"));
				}

			}
		}

		return new Dimension(0, 0);
	}

	private static Dimension getDimensionsFromImageSize(String imageSize) {
		// The format is "widthxheight"
		String[] parts = imageSize.split("x");
		if (parts.length != 2) {
			return new Dimension(0, 0);
		}

		return new Dimension(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
	}

	public static long detectAndConvertTime(String time) {
		if (time == null) {
			return 0L;
		}

		if (time.endsWith("s")) {
			return Long.parseLong(time.substring(0, time.length() - 1));
		}

		if (time.contains(":")) {
			return convertHMSStringToSeconds(time);
		}

		return Long.parseLong(time);
	}

	public static long convertHMSStringToSeconds(String hms) {
		if (hms == null) {
			return 0L;
		}

		String[] hmsParts = hms.split(":");
		if (hmsParts.length != 3) {
			return 0L;
		}

		long hours   = Long.parseLong(hmsParts[0]);
		long minutes = Long.parseLong(hmsParts[1]);
		long seconds = Long.parseLong(hmsParts[2]);

		return hours * 3600 + minutes * 60 + seconds;
	}
}
