package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.poltsi.vempain.auth.api.response.AbstractResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Page data")
public class PageResponse extends AbstractResponse {
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
	@Schema(description = "Date when the page was last published, null if never", example = "2022-05-13T16:03:44Z")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private Instant published;
}
