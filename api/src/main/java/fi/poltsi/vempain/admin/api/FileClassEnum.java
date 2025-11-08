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
	// Explicit mimetype -> class mapping for common types that don't map cleanly by top-level type
	private static final Map<String, FileClassEnum> BY_MIMETYPE    = new HashMap<>();

	static {
		for (FileClassEnum fcm : values()) {
			BY_ORDER.put(fcm.order, fcm);
			BY_DESCRIPTION.put(fcm.description, fcm);
			BY_NAME.put(fcm.shortName, fcm);
		}
	}

	// Populate common mimetypes per class
	static {
		// Documents (Office, ODF, PDF, HTML/MD/TXT/RTF)
		registerMime(DOCUMENT,
					 "application/pdf",
					 "application/msword",
					 "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
					 "application/vnd.ms-excel",
					 "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
					 "application/vnd.ms-powerpoint",
					 "application/vnd.openxmlformats-officedocument.presentationml.presentation",
					 "application/rtf",
					 "application/epub+zip",
					 "application/vnd.oasis.opendocument.text",
					 "application/vnd.oasis.opendocument.spreadsheet",
					 "application/vnd.oasis.opendocument.presentation",
					 "text/plain",
					 "text/markdown",
					 "text/html",
					 "text/rtf"
		);

		// Archives and compressed
		registerMime(ARCHIVE,
					 "application/zip",
					 "application/gzip",
					 "application/x-bzip2",
					 "application/x-7z-compressed",
					 "application/x-rar-compressed",
					 "application/x-tar",
					 "application/x-xz"
		);

		// Executables, installers, scripts
		registerMime(EXECUTABLE,
					 "application/x-msdownload",              // .exe
					 "application/x-dosexec",
					 "application/x-executable",              // ELF
					 "application/x-sharedlib",
					 "application/x-msi",                     // .msi
					 "application/vnd.android.package-archive", // .apk
					 "application/java-archive",              // .jar
					 "application/x-sh",                      // shell script
					 "text/x-shellscript",                    // shell script
					 "application/x-bat"                      // batch
		);

		// Interactive (Flash/Shockwave etc.)
		registerMime(INTERACTIVE,
					 "application/x-shockwave-flash",         // .swf
					 "application/x-director"                 // Shockwave
		);

		// Data (structured text/binary data formats)
		registerMime(DATA,
					 "application/json",
					 "application/xml",
					 "text/xml",
					 "text/csv",
					 "application/csv",
					 "application/x-ndjson",
					 "application/yaml",
					 "text/yaml",
					 "application/x-yaml",
					 "application/vnd.geo+json"
		);

		// Vector graphics
		registerMime(VECTOR,
					 "image/svg+xml",
					 "application/postscript",                // .ps / .eps
					 "application/eps",
					 "application/x-eps",
					 "application/vnd.adobe.illustrator"      // .ai
		);

		// Icons
		registerMime(ICON,
					 "image/vnd.microsoft.icon",
					 "image/x-icon"                           // .ico
		);

		// Fonts that sometimes appear under application/*
		registerMime(FONT,
					 "application/font-woff",
					 "application/font-woff2",
					 "application/x-font-ttf",
					 "application/x-font-otf"
		);

		// Generic binary
		registerMime(BINARY,
					 "application/octet-stream",
					 "application/x-binary"
		);

		// Thumbnails (non-standard but seen in the wild)
		registerMime(THUMB,
					 "image/x-thumbnail",
					 "application/x-thumbnail"
		);
	}

	public final  String shortName;
	private final long   order;
	private final String description;

	FileClassEnum(long order, String description, String shortName) {
		this.order = order;
		this.description = description;
		this.shortName = shortName;
	}

	private static void registerMime(FileClassEnum clazz, String... mimeTypes) {
		for (String m : mimeTypes) {
			if (m != null) {
				BY_MIMETYPE.put(m.toLowerCase(Locale.ROOT), clazz);
			}
		}
	}

	public static FileClassEnum getFileClassByOrder(long order) {
		return BY_ORDER.get(order);
	}

	public static FileClassEnum getFileClassByMimetype(String mimetype) {
		if (mimetype == null || mimetype.isBlank()) {
			return UNKNOWN;
		}
		final String mt = mimetype.trim()
								  .toLowerCase(Locale.ROOT);

		// 1) Explicit known mimetypes
		FileClassEnum mapped = BY_MIMETYPE.get(mt);
		if (mapped != null) {
			return mapped;
		}

		// 2) Top-level direct mappings
		var type = mt.split("/")[0];
		if (type.equals("image") ||
			type.equals("audio") ||
			type.equals("video") ||
			type.equals("font")) {
			return BY_NAME.get(type);
		}

		// 3) Text defaults to document unless explicitly overridden (e.g., shellscript handled above)
		if (type.equals("text")) {
			return DOCUMENT;
		}

		// 4) Remaining common families
		if (mt.contains("application/vnd.ms-") ||
			mt.contains("application/vnd.openxmlformats-officedocument.") ||
			mt.contains("application/vnd.oasis.opendocument")) {
			return DOCUMENT;
		}

		if (mt.equals("application/gzip") ||
			mt.equals("application/x-bzip2") ||
			mt.equals("application/zip") ||
			mt.equals("application/x-7z-compressed") ||
			mt.equals("application/x-rar-compressed") ||
			mt.equals("application/x-tar") ||
			mt.equals("application/x-xz")) {
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
