package fi.poltsi.vempain.admin.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Request to process files in a directory")
public class FileProcessRequest {
	@NotNull
	@Schema(description = "Source directory path on admin server side, relative to configured root", example = "/path/to/images")
	private String  sourceDirectory;
	@NotNull
	@Schema(description = "Destination directory path on site server side, relative to configured root", example = "/path/to/images")
	private String  destinationDirectory;
	@NotNull
	@Schema(description = "Toggle whether a gallery should also be created", example = "true")
	private boolean generateGallery;
	@Schema(description = "Short name of the gallery, if it should be created", example = "A gallery")
	private String  galleryShortname;
	@Schema(description = "Description of the gallery, if it should be created", example = "A pleasant gallery")
	private String  galleryDescription;
	@NotNull
	@Schema(description = "Toggle whether a page should also be created", example = "true")
	private boolean generatePage;
	@Schema(description = "Page title, if it should be created", example = "A gallery page")
	private String  pageTitle;
	@Schema(description = "Path of the gallery page, if it should be created", example = "/path/to/page")
	private String  pagePath;
	@Schema(description = "Gallery page body this is located above the gallery component itself, if it should be created", example = "A pleasant gallery")
	private String  pageBody;
	@Schema(description = "Form ID of the gallery page, if it should be created", example = "1")
	private Long pageFormId;
	@Schema(description = "Should the file processing be scheduled instead", example = "true")
	private boolean schedule;
}
