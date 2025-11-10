package fi.poltsi.vempain.site.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "web_site_file")
public class WebSiteFile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	protected long id;

	@Column(name = "file_id")
	protected long fileId;

	@Column(name = "acl_id")
	protected long aclId;

	@Column(name = "comment")
	private String comment;

	@Column(name = "path", nullable = false)
	private String path;

	@Column(name = "mimetype", nullable = false)
	private String mimetype;

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
	private long width;

	@Column(name = "height")
	private long height;

	@Column(name = "length")
	private long length;

	@Column(name = "pages")
	private long pages;

	@Column(name = "metadata", nullable = false)
	private String metadata;
}
