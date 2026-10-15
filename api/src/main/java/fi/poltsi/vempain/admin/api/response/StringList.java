package fi.poltsi.vempain.admin.api.response;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@EqualsAndHashCode
@SuperBuilder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Generic response class to send back a list of strings")
public class StringList {

    @Schema(description = "List of strings", example = "['ab', 'ba', 'ca']", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private List<String> stringList;
}
