package fi.poltsi.vempain.admin.api.request.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import fi.poltsi.vempain.admin.api.request.BaseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Gallery request")
public class GalleryRequest extends BaseRequest {
	@Schema(description = "Short name of the Gallery", example = "My Gallery")
	private String                        shortName;
	@Schema(description = "Description of the Gallery", example = "Gallery of our trip to Venezuela")
	private String            description;
	@Schema(description = "List of common file IDs that belong to this gallery", example = "[1,2,3,4]")
	private long[] commonFilesId;
}
