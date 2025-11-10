package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.file.api.response.LocationResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gps_location")
public class GpsLocation {

	@Id
	@Column(name = "id", nullable = false)
	private Long id; // original/external ID provided by upstream

	@Column(name = "latitude", precision = 15, scale = 5, nullable = false)
	private BigDecimal latitude;

	@Column(name = "latitude_ref", length = 1, nullable = false)
	private Character latitudeRef;

	@Column(name = "longitude", precision = 15, scale = 5, nullable = false)
	private BigDecimal longitude;

	@Column(name = "longitude_ref", length = 1, nullable = false)
	private Character longitudeRef;

	@Column(name = "altitude")
	private Double altitude;

	@Column(name = "direction")
	private Double direction;

	@Column(name = "satellite_count")
	private Integer satelliteCount;

	@Column(name = "country", length = 255)
	private String country;

	@Column(name = "state", length = 255)
	private String state;

	@Column(name = "city", length = 255)
	private String city;

	@Column(name = "street", length = 255)
	private String street;

	@Column(name = "sub_location", length = 255)
	private String subLocation;

	public LocationResponse toResponse() {
		return LocationResponse.builder()
							   .id(id)
							   .latitude(latitude)
							   .latitudeRef(latitudeRef)
							   .longitude(longitude)
							   .longitudeRef(longitudeRef)
							   .altitude(altitude)
							   .direction(direction)
							   .satelliteCount(satelliteCount)
							   .country(country)
							   .state(state)
							   .city(city)
							   .street(street)
							   .subLocation(subLocation)
							   .build();
	}
}
