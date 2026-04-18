package fi.poltsi.vempain.admin.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Request for creating or updating a CSV data set")
public class DataRequest {
	@Schema(description = "Unique identifier for the data set; must start with a letter and contain only lowercase letters, numbers, and underscores",
			example = "cd_collection")
	private String identifier;

	@Schema(description = "Type of the data set", example = "tabulated")
	private String type;

	@Schema(description = "Human-readable description of the data set", example = "Collection of music CDs")
	private String description;

	@Schema(description = "JSON array describing each column name and its data type",
			example = "[{\"name\":\"title\",\"type\":\"string\"},{\"name\":\"year\",\"type\":\"integer\"}]")
	private String columnDefinitions;

	@Schema(description = "SQL statement used to create the table in the site database",
			example = "CREATE TABLE website_data__cd_collection (id BIGSERIAL PRIMARY KEY, title VARCHAR(255), year INTEGER)")
	private String createSql;

	@Schema(description = "SQL statement used to fetch all rows from the site database table",
			example = "SELECT * FROM website_data__cd_collection ORDER BY id")
	private String fetchAllSql;

	@Schema(description = "SQL statement used to fetch a filtered subset of rows from the site database table",
			example = "SELECT * FROM website_data__cd_collection WHERE year >= :year_from AND year <= :year_to")
	private String fetchSubsetSql;

	@Schema(description = "Timestamp indicating when the data was generated", example = "2024-01-15T10:30:00Z")
	private Instant generated;

	@Schema(description = "Raw CSV data including a header row", example = "title,year\nAbbey Road,1969\nOk Computer,1997")
	private String csvData;
}
