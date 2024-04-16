package fi.poltsi.vempain.admin.entity;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageGallery implements Serializable {
	@Column(name = "sort_order")
	private Long sortOrder;
	@Column(name = "page_id")
	private Long pageId;
	@Column(name = "gallery_id")
	private Long galleryId;
}
