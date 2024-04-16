package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.admin.api.response.file.FileVideoResponse;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "file_video")
public class FileVideo extends AbstractFileImageEntity implements Serializable, FileEntityInterface<FileVideoResponse> {
	@Basic
	@Column(name = "length")
	private   long   length;

	@Override
	public FileVideoResponse toResponse() {
		return FileVideoResponse.builder()
								.id(this.getId())
								.length(this.getLength())
								.common(this.getFileCommon().toResponse())
								.build();
	}
}
