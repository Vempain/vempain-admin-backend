package fi.poltsi.vempain.admin.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Page request")
public class PageRequest extends BaseRequest {
	@Schema(description = "Parent page ID", example = "1")
	private Long    parentId;
	@Schema(description = "Form ID", example = "1")
	private Long    formId;
	@Schema(description = "URL path", example = "1")
	private String  path;
	@Schema(description = "Does the page require HTTPS", example = "true")
	private boolean secure;
	@Schema(description = "Should this page contain an index of the URL path", example = "true")
	private boolean indexList;
	@Schema(description = "Title text", example = "This is a title")
	private String  title;
	@Schema(description = "Header text", example = "This is the header")
	private String  header;
	@Schema(description = "The body text of the page", example = "<H1>Hello world!</H1>")
	private String  body;
}
