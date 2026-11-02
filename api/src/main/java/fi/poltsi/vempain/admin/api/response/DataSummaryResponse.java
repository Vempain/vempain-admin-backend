package fi.poltsi.vempain.admin.api.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import fi.poltsi.vempain.auth.api.response.AbstractResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * A data set without its CSV content. Data sets are ACL-bearing resources: the inherited {@code acls}, {@code creator}, {@code created}, {@code modifier} and
 * {@code modified} describe who owns and last changed the set in the admin backend; {@code acl_id} names its ACL.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Summary response for a CSV data set without the raw CSV data")
public class DataSummaryResponse extends AbstractResponse {

    @Schema(description = "ID of the ACL protecting the data set", example = "42")
    private long aclId;

    @Schema(description = "Unique identifier for the data set", example = "cd_collection")
    private String identifier;

    @Schema(description = "Type of the data set", example = "tabulated")
    private String type;

    @Schema(description = "Human-readable description of the data set", example = "Collection of music CDs")
    private String description;

    @Schema(description = "JSON array describing each column name and its data type",
            example = "[{\"name\":\"title\",\"type\":\"string\"},{\"name\":\"year\",\"type\":\"integer\"}]")
    private String columnDefinitions;

    @Schema(description = "SQL statement used to create the table in the site database")
    private String createSql;

    @Schema(description = "SQL statement used to fetch all rows from the site database table")
    private String fetchAllSql;

    @Schema(description = "SQL statement used to fetch a filtered subset of rows from the site database table")
    private String fetchSubsetSql;

    @Schema(description = "Timestamp indicating when the data was generated", example = "2024-01-15T10:30:00Z")
    private Instant generated;

    @Schema(description = "Timestamp when the data set was last published to the website, null if never", example = "2024-01-15T10:30:00Z")
    private Instant published;
}
