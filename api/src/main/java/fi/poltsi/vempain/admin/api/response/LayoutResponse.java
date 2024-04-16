package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import fi.poltsi.vempain.admin.api.request.AclRequest;
import fi.poltsi.vempain.admin.api.request.LayoutRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Item depicting a layout item")
public class LayoutResponse extends AbstractResponse {
	@Schema(description = "Layout ID", example = "1")
	private long   id;
	@Schema(description = "Layout name", example = "My layout")
	private String layoutName;
	@Schema(description = "Layout structure", example = "<!--comp_1-->")
	private String structure;

	@JsonIgnore
	public LayoutRequest getLayoutRequest() {
		List<AclRequest> aclRequests = new ArrayList<>();

		for (AclResponse aclResponse : this.getAcls()) {
			aclRequests.add(aclResponse.toRequest());
		}

		return LayoutRequest.builder()
							.id(this.id)
							.layoutName(this.layoutName)
							.structure(this.structure)
							.acls(aclRequests)
							.build();
	}
}
