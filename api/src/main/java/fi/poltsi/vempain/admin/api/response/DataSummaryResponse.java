package fi.poltsi.vempain.admin.api.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Summary response for a CSV data set without the raw CSV data")
public class DataSummaryResponse {
	@Schema(description = "Internal database ID", example = "1")
	private Long id;

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

	@Schema(description = "Timestamp when this record was created in the admin database")
	private Instant createdAt;

	@Schema(description = "Timestamp when this record was last updated in the admin database")
	private Instant updatedAt;
}
