package fi.poltsi.vempain.admin.api.response.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "List of file import schedules")
public class FileImportScheduleResponse {
	@Schema(description = "ID of the file import schedule", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
	private long id;

	@Schema(description = "Source directory path on admin server side, relative to configured root",
			example = "/path/to/images",
			requiredMode = Schema.RequiredMode.REQUIRED)
	private String sourceDirectory;

	@Schema(description = "Destination directory path on site server side, relative to configured root",
			example = "/path/to/images",
			requiredMode = Schema.RequiredMode.REQUIRED)
	private String destinationDirectory;

	@Schema(description = "Toggle whether a gallery should also be created", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
	private boolean generateGallery;

	@Schema(description = "Short name of the gallery, if it should be created", example = "A gallery")
	private String galleryShortname;

	@Schema(description = "Description of the gallery, if it should be created", example = "A pleasant gallery")
	private String galleryDescription;

	@Schema(description = "Toggle whether a page should also be created", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
	private boolean generatePage;

	@Schema(description = "Page title, if it should be created", example = "A gallery page")
	private String pageTitle;

	@Schema(description = "Path of the gallery page, if it should be created", example = "/path/to/page")
	private String pagePath;

	@Schema(description = "Gallery page body this is located above the gallery component itself, if it should be created", example = "A pleasant gallery")
	private String pageBody;

	@Schema(description = "Form ID of the gallery page, if it should be created", example = "1")
	private Long pageFormId;
}
