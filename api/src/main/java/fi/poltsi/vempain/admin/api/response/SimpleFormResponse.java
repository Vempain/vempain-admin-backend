package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Item depicting an ACL permission for either an user or unit")
public class SimpleFormResponse {
	@Schema(description = "Form ID", example = "1")
	@NotBlank
	private Long          formId;
	@Schema(description = "Form name", example = "My form")
	private String        name;
	@Schema(description = "Layout ID", example = "1")
	private Long          layoutId;
	@Schema(description = "ACL ID", example = "1")
	private Long    aclId;
	@Schema(description = "Whether the form should be locked from editing", example = "false")
	private boolean locked;
	@Schema(description = "User ID of the creator", example = "1")
	private Long    creator;
	@Schema(description = "User ID of the last modifier", example = "10")
	private Long          modifier;
	@Schema(description = "When was the form created", example = "2020-03-15 13:30:45")
	private LocalDateTime created;
	@Schema(description = "When was the form last modified", example = "2020-04-12 03:54:12")
	private LocalDateTime modified;
}
