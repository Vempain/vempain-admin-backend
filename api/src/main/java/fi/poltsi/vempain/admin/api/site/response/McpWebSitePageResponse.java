package fi.poltsi.vempain.admin.api.site.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "A page published to the site database")
public class McpWebSitePageResponse {

    private long id;
    private long aclId;
    private long pageId;
    private Long parentId;
    private String filePath;
    private boolean secure;
    private boolean indexList;
    private String title;
    private String header;
    private String body;
    private String pageStyle;
    private String creator;
    private Instant created;
    private String modifier;
    private Instant modified;
    private Instant published;
}
