package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.admin.api.response.file.FileDocumentResponse;
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
@Table(name = "file_document")
public class FileDocument extends AbstractFileEntity implements Serializable, FileEntityInterface<FileDocumentResponse> {
	@Basic
	@Column(name = "pages")
	private long pages;

	@Override
	public FileDocumentResponse toResponse() {
		return FileDocumentResponse.builder()
								   .id(this.getId())
								   .pages(this.getPages())
								   .common(this.getFileCommon().toResponse())
								   .build();
	}
}
