package fi.poltsi.vempain.admin.api.response.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import fi.poltsi.vempain.auth.api.response.AbstractResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Response containing the common file data")
public class FileCommonResponse extends AbstractResponse {
	@Schema(description = "Mimetype of the file", example = "text/plain", requiredMode = Schema.RequiredMode.REQUIRED)
	private String mimetype;

	@Schema(description = "File class ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	private long   fileClassId;

	@Schema(description = "Comment attached to the common file", example = "A red reindeer", requiredMode = Schema.RequiredMode.REQUIRED)
	private String comment;

	// Converted file details
	@Schema(description = "Converted file location", example = "/path/to/map.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
	private String convertedFile;

	@Schema(description = "Converted file size, in bytes", example = "8070", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long convertedFilesize;

	@Schema(description = "The SHA1 sum of the converted file, used for integrity", example = "4227b563bba45d20eae2f32aa2a1f03173401154",
			requiredMode = Schema.RequiredMode.REQUIRED)
	private String convertedSha1sum;

	@Schema(description = "Date and time stamp of when the file was created/image was taken, if available", example = "2022-05-13T16:03:44Z")
	private Instant originalDateTime;

	@Schema(description = "Second fraction when the file was created/image was taken, if available", example = "2022-05-13T16:03:44Z")
	private Integer originalSecondFraction;

	@Schema(description = "Unique document identifier, if available", example = "xmp.did:73200E145133DE118B6FB5810F8D53E5")
	private String originalDocumentId;

	// Site file details
	@Schema(description = "Name of the file, including the file suffix on the site", example = "map.jpg", requiredMode =
			Schema.RequiredMode.REQUIRED)
	private String siteFilename;

	@Schema(description = "Relative, to the setting, file path to the image file on site-side. If in root then the path is /",
			example = "/storage/images", requiredMode = Schema.RequiredMode.REQUIRED)
	private String siteFilepath;

	@Schema(description = "Site file size, in bytes", example = "8070", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long siteFilesize;

	@Schema(description = "The SHA1 sum of the site file, used for integrity", example = "4227b563bba45d20eae2f32aa2a1f03173401154",
			requiredMode = Schema.RequiredMode.REQUIRED)
	private String siteSha1sum;

	@Schema(description = "Metadata extracted from the file, in JSON format", example = "{\"filename\": \"map.jpg\"}",
			requiredMode = Schema.RequiredMode.REQUIRED)
	private String metadata;

}
