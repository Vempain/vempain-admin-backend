package fi.poltsi.vempain.admin.api.response.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(name = "FileIngestResponse", description = "Result of file ingest operation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileIngestResponse {
	@Schema(description = "ID of the stored site file", example = "12345")
	private Long siteFileId;

	@Schema(description = "Whether the operation updated an existing file", example = "true")
	private boolean updated;
}
