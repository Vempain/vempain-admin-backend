package fi.poltsi.vempain.admin.entity.file;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GalleryFile implements Serializable {
	@Column(name = "sort_order")
	private Long sortOrder;
	@Column(name = "gallery_id")
	private Long galleryId;
	@Column(name = "file_common_id")
	private Long fileCommonId;
}
