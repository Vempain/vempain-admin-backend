package fi.poltsi.vempain.admin.entity;

import fi.poltsi.vempain.admin.api.response.DataResponse;
import fi.poltsi.vempain.admin.api.response.DataSummaryResponse;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "data_store")
public class DataEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Basic
	@Column(name = "identifier", nullable = false, unique = true)
	private String identifier;

	@Basic
	@Column(name = "type", nullable = false)
	private String type;

	@Basic
	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Basic
	@Column(name = "column_definitions", nullable = false, columnDefinition = "TEXT")
	private String columnDefinitions;

	@Basic
	@Column(name = "create_sql", nullable = false, columnDefinition = "TEXT")
	private String createSql;

	@Basic
	@Column(name = "fetch_all_sql", nullable = false, columnDefinition = "TEXT")
	private String fetchAllSql;

	@Basic
	@Column(name = "fetch_subset_sql", nullable = false, columnDefinition = "TEXT")
	private String fetchSubsetSql;

	@Basic
	@Column(name = "data_timestamp", nullable = false)
	private Instant dataTimestamp;

	@Basic
	@Column(name = "csv_data", nullable = false, columnDefinition = "TEXT")
	private String csvData;

	@Basic
	@Column(name = "created_at")
	private Instant createdAt;

	@Basic
	@Column(name = "updated_at")
	private Instant updatedAt;

	public DataResponse toDataResponse() {
		return DataResponse.builder()
						   .id(this.id)
						   .identifier(this.identifier)
						   .type(this.type)
						   .description(this.description)
						   .columnDefinitions(this.columnDefinitions)
						   .createSql(this.createSql)
						   .fetchAllSql(this.fetchAllSql)
						   .fetchSubsetSql(this.fetchSubsetSql)
						   .dataTimestamp(this.dataTimestamp)
						   .csvData(this.csvData)
						   .createdAt(this.createdAt)
						   .updatedAt(this.updatedAt)
						   .build();
	}

	public DataSummaryResponse toDataSummaryResponse() {
		return DataSummaryResponse.builder()
								  .id(this.id)
								  .identifier(this.identifier)
								  .type(this.type)
								  .description(this.description)
								  .columnDefinitions(this.columnDefinitions)
								  .createSql(this.createSql)
								  .fetchAllSql(this.fetchAllSql)
								  .fetchSubsetSql(this.fetchSubsetSql)
								  .dataTimestamp(this.dataTimestamp)
								  .createdAt(this.createdAt)
								  .updatedAt(this.updatedAt)
								  .build();
	}
}
