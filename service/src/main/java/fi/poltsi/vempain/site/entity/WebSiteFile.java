package fi.poltsi.vempain.site.entity;

import fi.poltsi.vempain.common.DurationToLongConverter;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Duration;
import java.time.Instant;

@Data
@Entity
@Builder
@ToString(exclude = "metadata")
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "web_site_file")
public class WebSiteFile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	protected long id;

	@Column(name = "file_id", nullable = false)
	protected long fileId;

	@Column(name = "acl_id", nullable = false)
	protected long aclId;

	@Column(name = "comment")
	private String comment;

	@Column(name = "file_path", nullable = false)
	private String filePath;

	@Column(name = "mimetype", nullable = false)
	private String mimetype;

	@Enumerated(EnumType.STRING)
	@Column(name = "file_type", nullable = false)
	private FileTypeEnum fileType;

	@Column(name = "original_datetime")
	private Instant originalDateTime;

	@Column(name = "rights_holder")
	private String rightsHolder;

	@Column(name = "rights_terms")
	private String rightsTerms;

	@Column(name = "rights_url", length = 1024)
	private String rightsUrl;

	@Column(name = "creator_name")
	private String creatorName;

	@Column(name = "creator_email")
	private String creatorEmail;

	@Column(name = "creator_country")
	private String creatorCountry;

	@Column(name = "creator_url", length = 1024)
	private String creatorUrl;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id")
	private WebGpsLocation location;

	@Column(name = "width")
	private Integer width;

	@Column(name = "height")
	private Integer height;

	@Convert(converter = DurationToLongConverter.class)
	@Column(name = "length")
	private Duration length;

	@Column(name = "pages")
	private Integer pages;

	@Column(name = "metadata", nullable = false)
	private String metadata;
}
