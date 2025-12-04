package fi.poltsi.vempain.admin.api.request.file;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Request payload containing gallery IDs to publish")
public class GalleryPublishRequest {
	@NotEmpty
	@Schema(description = "Gallery IDs to publish", example = "[1,2,3]")
	private List<Long> galleryIds;
}

