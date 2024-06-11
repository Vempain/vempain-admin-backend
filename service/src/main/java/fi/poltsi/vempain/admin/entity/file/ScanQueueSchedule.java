package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.admin.api.response.file.FileImportScheduleResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "scan_queue_schedule")
public class ScanQueueSchedule {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@NotNull
	@Column(name = "source_directory", nullable = false, length = Integer.MAX_VALUE)
	private String sourceDirectory;

	@NotNull
	@Column(name = "destination_directory", nullable = false, length = Integer.MAX_VALUE)
	private String destinationDirectory;

	@NotNull
	@Column(name = "create_gallery", nullable = false)
	private boolean createGallery;

	@Column(name = "gallery_shortname")
	@Size(min = 2, max = 256, message = "Description should be between 2 and 256 characters")
	private String galleryShortname;

	@Column(name = "gallery_description", length = 2000)
	@Size(min = 2, max = 2000, message = "Description should be between 2 and 2000 characters")
	private String galleryDescription;

	@NotNull
	@Column(name = "create_page", nullable = false)
	private boolean createPage;

	@Size(max = 512)
	@Column(name = "page_title", nullable = false, length = 512)
	private String pageTitle;

	@Size(max = 255)
	@Column(name = "page_path")
	private String pagePath;

	@Column(name = "page_body", length = 524288)
	private String pageBody;

	@NotNull
	@Column(name = "page_form_id")
	private Long pageFormId;

	@NotNull
	@Column(name = "created_by", nullable = false)
	private Long createdBy;

	@NotNull
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@NotNull
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public FileImportScheduleResponse toResponse() {
		return FileImportScheduleResponse.builder()
				.id(id)
				.sourceDirectory(sourceDirectory)
				.destinationDirectory(destinationDirectory)
				.generateGallery(createGallery)
				.galleryShortname(galleryShortname)
				.galleryDescription(galleryDescription)
				.generatePage(createPage)
				.pageTitle(pageTitle)
				.pagePath(pagePath)
				.pageBody(pageBody)
				.pageFormId(pageFormId)
				.build();
	}
}
