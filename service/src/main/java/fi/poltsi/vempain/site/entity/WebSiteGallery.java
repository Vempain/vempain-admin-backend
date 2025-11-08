package fi.poltsi.vempain.site.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
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
@Table(name = "web_site_gallery")
public class WebSiteGallery {
	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	@Column(name = "id")
	private   long    id;

	@Column(name = "gallery_id")
	private long galleryId;

	@Size(min = 2, max = 2000, message = "Description should be between 2 and 2000 characters")
	@Column(name = "description")
	private String description;

	@Size(min = 2, max = 256, message = "Description should be between 2 and 256 characters")
	@Column(name = "shortname")
	private String shortname;

	@Column(name = "creator", nullable = false)
	protected Long    creator;

	@Column(name = "created", nullable = false)
	protected Instant created;

	@Column(name = "modifier")
	protected Long    modifier;

	@Column(name = "modified")
	protected Instant modified;
}
