package fi.poltsi.vempain.admin.api.site.response;

import fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * A published page or file of the website that page content may link to. Only what exists on the site is listed, so a link inserted from these targets resolves
 * on the website.
 */
@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "A published page or file of the website that page content may link to")
@NoArgsConstructor
@AllArgsConstructor
public class InternalLinkResponse {

    @Schema(description = "What the target is: a published page or a published file", example = "PAGE", allowableValues = { "PAGE", "SITE_FILE" })
    WebSiteResourceEnum type;
    @Schema(description = "Title of the page, or the path of the file", example = "About us")
    String title;
    @Schema(description = "Path of the page on the website, or the path of the file below the file root (type/directory/name)", example = "about")
    String path;
}
