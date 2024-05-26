package fi.poltsi.vempain.tools;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class JsonTools {
	private static final String XMP_KEY             = "XMP";
	private static final String XMP_DESCRIPTION_KEY = "Description";
	private static final String IPTC_KEY            = "IPTC";
	private static final String IPTC_CAPTION_KEY    = "Caption-Abstract";
	private static final String IPTC_KEYWORD_KEY    = "Keywords";
	private static final String DATETIME_ORIGINAL   = "DateTimeOriginal";
	private static final String CREATE_DATE         = "CreateDate";
	private static final String MIMETYPE = "MIMEType";

	private JsonTools() {
	}

	/**
	 * Fetch full sub-section (XMP/IPTC...) of a metadata structure in JSON format
	 * @param parent Parent section of JSON
	 * @param key Key to be retrieved
	 * @return Retuns an Optional<JSONObject>
	 */
	public static Optional<JSONObject> getJSONObject(JSONObject parent, String key) {
		Optional<JSONObject> optional = Optional.empty();
		JSONObject           jsonObject;
		try {
			jsonObject = parent.getJSONObject(key);
			optional   = Optional.of(jsonObject);
		} catch (JSONException e) {
			log.error("Did not find {} in JSON object", key);
		}

		return optional;
	}

	/**
	 * Extract the comment/description field from given metadata
	 * @param jsonObject Extracted JSON formatted metadata (from @MetadataTools.getMetadataAsJSON())
	 * @return String value retrieved, or null if none were found
	 */
	public static String getDescriptionFromJson(JSONObject jsonObject) {
		Map<String, List<String>> locations = new HashMap<>();
		locations.put("XMP", List.of(XMP_DESCRIPTION_KEY));
		locations.put("IPTC", List.of(IPTC_CAPTION_KEY));

		return extractJsonString(jsonObject, locations);
	}

	/**
	 * Extract the mimetype from the given metadata
	 * @param jsonObject Extracted JSON formatted metadata (from @MetadataTools.getMetadataAsJSON())
	 * @return String value retrieved, or null if none were found
	 */
	public static String extractMimetype(JSONObject jsonObject) {
		Map<String, List<String>> locations = new HashMap<>();
		locations.put("File", List.of(MIMETYPE));
		locations.put("XMP", List.of(MIMETYPE));

		return extractJsonString(jsonObject, locations);
	}

	/**
	 * Extract the  original time of creation from the given metadata in JSON format
	 * @param jsonObject Extracted JSON formatted metadata (from @MetadataTools.getMetadataAsJSON())
	 * @return String value retrieved, or null if none were found
	 */
	public static String getOriginalDateTimeFromJson(JSONObject jsonObject) {
		Map<String, List<String>> locations = new HashMap<>();
		locations.put("ExifIFD", Arrays.asList(DATETIME_ORIGINAL, CREATE_DATE));
		locations.put("EXIF", List.of(DATETIME_ORIGINAL));
		locations.put("XMP-xmp", List.of(CREATE_DATE));
		locations.put("XMP", List.of(DATETIME_ORIGINAL));
		locations.put("XMP-exif", List.of(DATETIME_ORIGINAL));
		locations.put("Composite", Arrays.asList(DATETIME_ORIGINAL, "DigitalCreationDateTime", "SubSecCreateDate"));

		return extractJsonString(jsonObject, locations);
	}

	/**
	 * Extract the fraction of second of the original time of creation from the given metadata in JSON format
	 * @param jsonObject Extracted JSON formatted metadata (from @MetadataTools.getMetadataAsJSON())
	 * @return String value retrieved, or null if none were found
	 */

	public static int getOriginalSecondFraction(JSONObject jsonObject) {
		Map<String, List<String>> locations = new HashMap<>();
		locations.put("ExifIFD", Arrays.asList("SubSecTimeOriginal", "SubSecTimeDigitized"));
		var fraction = extractJsonNumber(jsonObject, locations);

		if (fraction != null) {
			return fraction.intValue();
		}

		return 0;
	}

	/**
	 * Extract the document ID from the given metadata in JSON format
	 * @param jsonObject Extracted JSON formatted metadata (from @MetadataTools.getMetadataAsJSON())
	 * @return String value retrieved, or null if none were found
	 */
	public static String getOriginalDocumentId(JSONObject jsonObject) {
		Map<String, List<String>> locations = new HashMap<>();
		locations.put("XMP-xmpMM", Arrays.asList("OriginalDocumentID", "DocumentID", "InstanceID", "DerivedFromOriginalDocumentID"));
		locations.put("XMP", Arrays.asList("OriginalDocumentID", "DocumentID", "InstanceID", "DerivedFromOriginalDocumentID"));
		return extractJsonString(jsonObject, locations);
	}

	/**
	 * Extract the list of subjects/keywords from the given metadata in JSON format
	 * @param jsonObject Extracted JSON formatted metadata (from @MetadataTools.getMetadataAsJSON())
	 * @return String value retrieved, or null if none were found
	 */
	public static List<String> getSubjects(JSONObject jsonObject) {
		Map<String, List<String>> locations = new HashMap<>();
		locations.put(XMP_KEY, List.of("Subject"));
		locations.put("XMP-dc", List.of("Subject"));
		locations.put("XMP-lr", List.of("HierarchicalSubject"));
		locations.put(IPTC_KEY, List.of(IPTC_KEYWORD_KEY));

		// We try first to extract an array of strings
		var subjectList= extractJsonArray(jsonObject, locations);

		// If the array is empty, we try to extract a single string
		if (subjectList.isEmpty()) {
			var subject = extractJsonString(jsonObject, locations);
			if (subject != null && !subject.isBlank()) {
				subjectList.add(subject);
			}
		}

		return subjectList;
	}

	private static String extractJsonString(JSONObject jsonObject, Map<String, List<String>> locations) {
		for (Map.Entry<String, List<String>> location : locations.entrySet()) {
			for (String key : location.getValue()) {
				if (jsonObject.has(location.getKey()) && jsonObject.getJSONObject(location.getKey()).has(key)) {
					return jsonObject.getJSONObject(location.getKey()).getString(key);
				}
			}
		}

		return null;
	}

	private static Number extractJsonNumber(JSONObject jsonObject, Map<String, List<String>> locations) {
		for (Map.Entry<String, List<String>> location : locations.entrySet()) {
			for (String key : location.getValue()) {
				if (jsonObject.has(location.getKey()) && jsonObject.getJSONObject(location.getKey()).has(key)) {

					Number number;
					try {
						number = jsonObject.getJSONObject(location.getKey()).getNumber(key);
						return number;
					} catch (JSONException e) {
						log.warn("Failed to retrieve JSON number from location {}, trying to get it as String instead", key);
						var stringValue = jsonObject.getJSONObject(location.getKey()).getString(key);

						try {
							return Integer.parseInt(stringValue);
						} catch (NumberFormatException ex) {
							log.error("Key {} exists but can not be parsed as number", key);
						}
					}
				}
			}
		}

		return null;
	}

	public static List<String> extractJsonArray(JSONObject jsonObject, Map<String, List<String>> locations) {
		for (Map.Entry<String, List<String>> location : locations.entrySet()) {
			for (String key : location.getValue()) {
				if (jsonObject.has(location.getKey()) && jsonObject.getJSONObject(location.getKey()).has(key)) {
					try {
						var objectList = jsonObject.getJSONObject(location.getKey()).getJSONArray(key).toList();
						return objectList.stream().map(o -> Objects.toString(o, null)).toList();
					} catch (JSONException e) {
						log.error("Failed to retrieve JSON array from location {}, value is: {}", key, jsonObject.getJSONObject(location.getKey()));
					}
				}
			}
		}

		return new ArrayList<>();
	}
}
