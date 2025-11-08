package fi.poltsi.vempain.site.entity;

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
