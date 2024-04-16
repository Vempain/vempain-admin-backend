package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.admin.api.response.file.FileAudioResponse;
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
@Table(name = "file_audio")
public class FileAudio extends AbstractFileEntity implements Serializable, FileEntityInterface<FileAudioResponse> {
	@Basic
	@Column(name = "length")
	private long length;

	@Override
	public FileAudioResponse toResponse() {
		return FileAudioResponse.builder()
								.id(this.getId())
								.length(this.getLength())
								.common(this.getFileCommon().toResponse())
								.build();
	}
}
