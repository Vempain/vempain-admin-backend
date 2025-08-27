package fi.poltsi.vempain.admin.api.request.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "FileIngestRequest", description = "Metadata for ingesting a file into site storage")
public class FileIngestRequest {
	@Schema(description = "Target file name", example = "img_001.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 255)
	private String fileName;           // required

	@Schema(description = "Relative directory path under the mimetype class directory", example = "gallery/2025/08")
	@Size(max = 2048)
	private String filePath;           // optional subpath, may be empty

	@Schema(description = "RFC 2046 MIME type", example = "image/jpeg", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 255)
	@Pattern(regexp = "^[^/\\s]+/[^/\\s]+$", message = "Must be in 'type/subtype' format")
	private String mimeType;           // required

	@Schema(description = "Comment", example = "Some comment", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	@NotNull
	private String   comment;

	@Schema(description = "Metadata of the file in JSON format", example = "{\"some-field\": \"some value\"}", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	@NotNull
	private String   metadata;

	@Schema(description = "SHA 256 sum of the sent file, used to verify the file integrity", example = "a", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private String   sha256sum;

	@Schema(description = "User ID performing the ingest", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(1L)
	private Long   userId;             // required

	@Schema(description = "Existing gallery ID to associate with (if any)", example = "1001")
	@Positive
	private Long   galleryId;          // optional

	@Schema(description = "Gallery short name (created/updated if provided)", example = "Summer 2025")
	@Size(max = 255)
	private String galleryName;        // optional (short name)

	@Schema(description = "Gallery description (created/updated if provided)", example = "A sunny album from August 2025")
	@Size(max = 2048)
	private String galleryDescription; // optional
}
