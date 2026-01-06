package fi.poltsi.vempain.admin.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.poltsi.vempain.auth.api.request.AclRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Default response for any delete action")
public abstract class BaseRequest {
	@Schema(description = "ID of the DTO", example = "123")
	private long id;
	@Schema(description = "Date time of the request", example = "2021.05.28T13:13:13.123Z+002")
	@Builder.Default
	private LocalDateTime    timestamp = LocalDateTime.now();
	@Schema(description = "List of ACL request", example = "[{AclRequest}]")
	private List<AclRequest> acls;
}
