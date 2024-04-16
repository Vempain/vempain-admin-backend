package fi.poltsi.vempain.admin.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Form request")
public class FormRequest extends BaseRequest {
	@Schema(description = "Form ID", example = "1")
	private long                   id;
	@Schema(description = "Form name", example = "My layout")
	private String                 name;
	@Schema(description = "Layout ID", example = "1")
	private long                   layoutId;
	@Schema(description = "List of component IDs, the order matters as the layout will be populated in this order", example = "[1,2,3]")
	private List<ComponentRequest> components;
	@Schema(description = "Whether the component should be locked from editing", example = "true")
	private boolean                locked;
}
