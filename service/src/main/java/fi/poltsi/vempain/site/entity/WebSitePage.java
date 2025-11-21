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

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "web_site_page")
public class WebSitePage {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	protected long    id;

	@Column(name = "acl_id")
	protected long aclId;

	@Column(name = "page_id")
	protected long    pageId;

	@Column(name = "parent_id")
	private   Long    parentId;

	@Column(name = "path", nullable = false)
	private   String  path;

	@Column(name = "secure", nullable = false)
	private   boolean secure;

	@Column(name = "indexlist", nullable = false)
	private   boolean indexList;

	@Column(name = "title", nullable = false, length = 512)
	private   String  title;

	@Column(name = "header", nullable = false, length = 512)
	private   String  header;

	@Column(name = "body", nullable = false, length = 524288)
	private   String  body;

	@Column(name = "creator", nullable = false, length = 512)
	private   String  creator;

	@Column(name = "created", nullable = false)
	private   Instant created;

	@Column(name = "modifier", length = 512)
	private   String  modifier;

	@Column(name = "modified")
	private   Instant modified;

	@Column(name = "cache", length = 524288)
	private   String  cache;

	@Column(name = "embeds")
	private String embeds;

	@Column(name = "published")
	private   Instant published;
}
