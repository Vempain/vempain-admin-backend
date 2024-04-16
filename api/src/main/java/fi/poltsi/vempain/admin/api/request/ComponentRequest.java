package fi.poltsi.vempain.admin.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Component request")
public class ComponentRequest extends BaseRequest {
	@Schema(description = "Component ID", example = "1")
	private long   id;
	@Schema(description = "Component name", example = "My component")
	private String compName;
	@Schema(description = "Component content", example = "<?php print(\"Hello world!\"); ?>")
	private String compData;
	@Schema(description = "Whether the component should be locked from editing", example = "NO")
	private boolean  locked;
}
