package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.admin.api.FileClassEnum;
import fi.poltsi.vempain.admin.api.response.file.FileThumbResponse;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "file_thumb")
public class FileThumb extends AbstractFileImageEntity implements Serializable, FileEntityInterface<FileThumbResponse> {
	@Basic
	@Enumerated(EnumType.STRING)
	@Column(name = "parent_class", nullable = false)
	private FileClassEnum parentClass;
	@Basic
	@Column(name = "filename", columnDefinition = "TEXT")
	private String        filename;
	@Basic
	@Column(name = "filepath", columnDefinition = "TEXT")
	private String filepath;
	@Basic
	@Column(name = "filesize")
	private Long          filesize;
	@Basic
	@Column(name = "sha1sum")
	private String sha1sum;

	@Override
	public FileThumbResponse toResponse() {
		return FileThumbResponse.builder()
								.id(this.getId())
								.parentId(this.getParentId())
								.parentClass(this.getParentClass())
								.width(this.getWidth())
								.height(this.getHeight())
								.siteFile(this.getSiteFile()
											  .toResponse())
								.build();
	}
}
