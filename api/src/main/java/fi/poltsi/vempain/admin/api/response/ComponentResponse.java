package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Item depicting a reusable component, usually written in PHP")
public class ComponentResponse extends AbstractResponse {
	@Schema(description = "Component ID", example = "1")
	private long   id;
	@Schema(description = "Component name", example = "My component")
	private String compName;
	@Schema(description = "Component content", example = "<?php print(\"Hello world!\"); ?>")
	private String compData;
}
