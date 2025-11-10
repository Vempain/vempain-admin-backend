package fi.poltsi.vempain.admin.api.response.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import fi.poltsi.vempain.file.api.response.LocationResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Response for a site file", title = "SiteFileResponse", oneOf = {SiteFileResponse.class})
public class SiteFileResponse {
	@Schema(description = "Unique identifier of the file", example = "1")
	private Long id;

	@Schema(description = "Name of the file", example = "example.jpg")
	private String fileName;

	@Schema(description = "Path to the file", example = "/uploads/example.jpg")
	private String filePath;

	@Schema(description = "File class", example = "IMAGE")
	private FileTypeEnum fileType;

	@Schema(description = "MIME type of the file", example = "image/jpeg")
	private String mimeType;

	@Schema(description = "Size of the file in bytes", example = "204800")
	private long size;

	@Schema(description = "sha256sum of the file", example = "a3f5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2g3h4i5j6k7l8m9n0o1p2q3r4s5")
	private String sha256sum;

	@Schema(description = "Creator ID of the file", example = "123")
	private long creator;

	@Schema(description = "Creation timestamp of the file", example = "2023-10-01T12:00:00Z")
	private Instant created;

	@Schema(description = "Last updater ID of the file", example = "123")
	private Long modifier;

	@Schema(description = "Last update timestamp of the file", example = "2023-10-01T12:00:00Z")
	private Instant modified;

	@Schema(description = "Free-form comment associated with the file", example = "Some comment")
	private String comment;

	@Schema(description = "Arbitrary metadata JSON stored with the file", example = "{\"some-field\":\"some value\"}")
	private String metadata;

	@Schema(description = "Original (source) datetime of the file", example = "2025-08-15T14:30:00Z")
	private Instant originalDateTime;

	@Schema(description = "Copyright holder name", example = "John Doe")
	private String rightsHolder;

	@Schema(description = "Copyright terms", example = "All rights reserved")
	private String rightsTerms;

	@Schema(description = "Copyright terms URL", example = "https://example.com/copyright")
	private String rightsUrl;

	@Schema(description = "Creator name", example = "John Doe")
	private String creatorName;

	@Schema(description = "Creator email", example = "john@example.com")
	private String creatorEmail;

	@Schema(description = "Creator country", example = "Finland")
	private String creatorCountry;

	@Schema(description = "Creator URL", example = "https://example.com/creator")
	private String creatorUrl;

	@Schema(description = "Optional GPS location data (shared between multiple files)")
	private LocationResponse location;
}
