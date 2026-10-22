package fi.poltsi.vempain.site.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "web_gps_location")
public class WebGpsLocation {

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

    /**
     * Nick of the admin user who created the row on the site side, null for rows published before the audit columns existed.
     */
    @Column(name = "creator", length = 512)
    private String creator;

    @Column(name = "created")
    private Instant created;

    /**
     * Nick of the admin user who last changed the row on the site side.
     */
    @Column(name = "modifier", length = 512)
    private String modifier;

    @Column(name = "modified")
    private Instant modified;
}
