package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.admin.api.FileClassEnum;
import fi.poltsi.vempain.admin.api.response.file.SiteFileResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Builder
@Getter
@Setter
@Entity
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "site_file", indexes = {
		@Index(name = "ux_site_file_path_name", columnList = "file_path, file_name", unique = true)
})
public class SiteFile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "file_path", nullable = false, length = 2048)
	private String filePath;

	@Column(name = "mime_type", nullable = false, length = 255)
	private String mimeType;

	@Column(name = "size", nullable = false)
	private long size;

	@Column(name = "file_class", nullable = false)
	private FileClassEnum fileClass;

	@Column(name = "comment", nullable = false)
	private String comment;

	@Column(name = "metadata", nullable = false)
	private String metadata;

	@Column(name = "sha256sum", nullable = false)
	private String sha256sum;

	@Column(name = "creator", length = 255)
	private long creator;

	@Column(name = "created")
	private Instant created;

	@Column(name = "modifier", length = 255)
	private Long modifier;

	@Column(name = "modified")
	private Instant modified;

	public SiteFileResponse toResponse() {
		return SiteFileResponse.builder()
							   .id(this.id)
							   .fileName(this.fileName)
							   .filePath(this.filePath)
							   .fileClass(this.fileClass.name())
							   .mimeType(this.mimeType)
							   .size(this.size)
							   .sha256sum(this.sha256sum)
							   .creator(this.creator)
							   .created(this.created)
							   .modifier(this.modifier)
							   .modified(this.modified)
							   .build();
	}
}

