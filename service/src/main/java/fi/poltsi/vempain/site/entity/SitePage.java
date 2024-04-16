package fi.poltsi.vempain.site.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "page")
public class SitePage {
	@Id
	@Column(name = "id")
	protected long    id;
	@Column(name = "parent_id")
	private   long   parentId;
	@Column(name = "path", nullable = false)
	private   String path;
	@Column(name = "secure", nullable = false)
	private   boolean  secure;
	@Column(name = "indexlist", nullable = false)
	private   boolean  indexList;
	@Column(name = "title", nullable = false, length = 512)
	private String title;
	@Column(name = "header", nullable = false, length = 512)
	private String header;
	@Column(name = "body", nullable = false, length = 524288)
	private String body;
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
	@Column(name = "published")
	private   Instant published;
}
