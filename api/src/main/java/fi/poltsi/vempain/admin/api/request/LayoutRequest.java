package fi.poltsi.vempain.admin.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Layout request")
public class LayoutRequest extends BaseRequest {
    @Schema(description = "Layout ID", example = "1")
    private long   id;
    @Schema(description = "Layout name", example = "My layout")
    private String layoutName;
    @Schema(description = "Layout structure", example = "<!--comp_1-->")
    private String structure;
}
