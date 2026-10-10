package fi.poltsi.vempain.admin.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * Auto-completion of a page path being typed: the parent path shared by every existing page whose path starts with the typed text.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "PagePathSuggestionResponse", description = "Suggested parent path for the page path being typed")
public class PagePathSuggestionResponse {
	@Schema(description = "The typed prefix the suggestion was computed for", example = "so")
	private String prefix;

	@Schema(description = "Parent path common to every matching page, without the last path part; null when no readable page matches "
						  + "or the matches share no common part", example = "some/path", nullable = true)
	private String suggestion;

	@Schema(description = "Number of readable pages whose path starts with the prefix", example = "2")
	private int matches;
}
