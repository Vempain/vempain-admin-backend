package fi.poltsi.vempain.admin.api;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
public enum FileClassEnum {

	UNKNOWN(0L, "Unknown filetype", "unknown"),
	BINARY(1L, "Binary file", "binary"),
	IMAGE(2L, "Bitmap image files", "image"),
	VECTOR(3L, "Vector image files", "vector"),
	AUDIO(4L, "Audio files", "audio"),
	VIDEO(5L, "Video files", "video"),
	DOCUMENT(6L, "Document files", "document"),
	ARCHIVE(7L, "(Un)Compressed archive files", "archive"),
	EXECUTABLE(8L, "Executable files including scripts", "executable"),
	INTERACTIVE(9L, "Interactive files (Flash, Shockwave etc)", "interactive"),
	DATA(10L, "Various data files, binary or ascii", "data"),
	FONT(11L, "Font files", "font"),
	ICON(12L, "Icon files", "icon"),
	THUMB(13L, "Thumb file", "thumb");

	private static final Map<Long, FileClassEnum>   BY_ORDER       = new HashMap<>();
	private static final Map<String, FileClassEnum> BY_DESCRIPTION = new HashMap<>();
	private static final Map<String, FileClassEnum> BY_NAME        = new HashMap<>();

	private final long   order;
	private final String description;
	public final  String shortName;

	static {
		for (FileClassEnum fcm : values()) {
			BY_ORDER.put(fcm.order, fcm);
			BY_DESCRIPTION.put(fcm.description, fcm);
			BY_NAME.put(fcm.shortName, fcm);
		}
	}

	FileClassEnum(long order, String description, String shortName) {
		this.order       = order;
		this.description = description;
		this.shortName   = shortName;
	}

	public static FileClassEnum getFileClassByOrder(long order) {
		return BY_ORDER.get(order);
	}

	public static FileClassEnum getFileClassByMimetype(String mimetype) {
		var type = mimetype.split("/")[0].toLowerCase(Locale.ROOT);

		if (type.equals("image") ||
			type.equals("audio") ||
			type.equals("video") ||
			type.equals("font")) {
			return BY_NAME.get(type);
		}

		if (type.equals("text") ||
			mimetype.equals("application/pdf") ||
			mimetype.contains("application/vnd.ms-excel") ||
			mimetype.contains("application/vnd.openxmlformats")) {
			return DOCUMENT;
		}

		if (mimetype.equals("application/gzip") ||
			mimetype.equals("application/x-bzip2") ||
			mimetype.equals("application/zip")) {
			return ARCHIVE;
		}

		return UNKNOWN;
	}

	public static long getFileClassIdByMimetype(String mimetype) {
		return getFileClassByMimetype(mimetype).order;
	}

	public static String getFileClassNameByMimetype(String mimetype) {
		return getFileClassByMimetype(mimetype).shortName;
	}

	public static Set<String> getFileClassNames() {
		return BY_NAME.keySet();
	}

	public static String getFileClassNameById(long fileClassId) {
		return BY_ORDER.get(fileClassId).shortName;
	}
}
