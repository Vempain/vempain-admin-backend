package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.admin.api.response.file.SiteFileResponse;
import fi.poltsi.vempain.auth.entity.AbstractVempainEntity;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Getter
@Setter
@Entity
@ToString(callSuper = true)
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "site_file", indexes = {
		@Index(name = "ux_site_file_path_name", columnList = "file_path, file_name", unique = true)
})
public class SiteFile extends AbstractVempainEntity implements Serializable {

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "file_path", nullable = false, length = 2048)
	private String filePath;

	@Column(name = "mime_type", nullable = false, length = 255)
	private String mimeType;

	@Column(name = "size", nullable = false)
	private long size;

	@Enumerated(EnumType.STRING)
	@Column(name = "file_type", nullable = false)
	private FileTypeEnum fileType;

	@Column(name = "comment", nullable = false)
	private String comment;

	@Column(name = "metadata", nullable = false)
	private String metadata;

	@Column(name = "sha256sum", nullable = false)
	private String sha256sum;

	public SiteFileResponse toResponse() {
		return SiteFileResponse.builder()
							   .id(this.id)
							   .fileName(this.fileName)
							   .filePath(this.filePath)
							   .fileType(this.fileType)
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

