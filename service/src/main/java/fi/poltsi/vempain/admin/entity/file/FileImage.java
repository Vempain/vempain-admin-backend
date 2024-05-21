package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.admin.api.response.file.FileImageResponse;
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
@Table(name = "file_image")
public class FileImage extends AbstractFileImageEntity implements Serializable, FileEntityInterface<FileImageResponse> {
	@Override
	public FileImageResponse toResponse() {
		return FileImageResponse.builder()
								.id(this.getId())
								.parentId(this.getParentId())
								.width(this.getWidth())
								.height(this.getHeight())
								.common(this.getFileCommon().toResponse())
								.build();
	}
}
