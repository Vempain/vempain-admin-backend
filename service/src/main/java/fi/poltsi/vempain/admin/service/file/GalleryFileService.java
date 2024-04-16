package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.entity.file.GalleryFile;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class GalleryFileService {
	private final EntityManager entityManager;
	private final FileThumbService fileThumbService;

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteAllGalleryFiles() {
		entityManager.joinTransaction();
		var query = entityManager.createNativeQuery("DELETE FROM gallery_file");
		query.executeUpdate();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteGalleryFile(Long galleryId, Long fileId, Long sortOrder) {
		var query = entityManager.createNativeQuery("DELETE FROM gallery_file " +
													  "WHERE gallery_id = :galleryId " +
													  "  AND file_common_id = :fileId " +
													  "  AND sort_order = :sortOrder");
		query.setParameter("galleryId", galleryId);
		query.setParameter("fileId", fileId);
		query.setParameter("sortOrder", sortOrder);
		query.executeUpdate();
	}

	public List<GalleryFile> findGalleryFileByGalleryId(Long galleryId) {
		var query = entityManager.createNativeQuery("SELECT gf.gallery_id, gf.file_common_id, gf.sort_order " +
													  "FROM gallery_file gf " +
													  "WHERE gf.gallery_id = :galleryId " +
													  "ORDER BY gf.sort_order");
		query.setParameter("galleryId", galleryId);
		List<Object[]>      gfObjects      = query.getResultList();
		List<GalleryFile> galleryFiles = new ArrayList<>();

		if (gfObjects.isEmpty()) {
			return new ArrayList<>();
		}

		for (Object[] o : gfObjects) {
			var intFormId = (Long) o[0];
			var intCompId = (Long) o[1];
			var intSortOrder = (Long) o[2];
			galleryFiles.add(GalleryFile.builder()
											.galleryId(intFormId )
											.fileCommonId(intCompId)
											.sortOrder(intSortOrder)
											.build());
		}

		return galleryFiles;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addGalleryFile(Long galleryId, Long fileCommonId, Long sortOrder) {
		var query = entityManager.createNativeQuery("INSERT INTO gallery_file (gallery_id, file_common_id, sort_order) " +
													  "VALUES (:galleryId, :fileCommonId, :sortOrder)");
		query.setParameter("galleryId", galleryId);
		query.setParameter("fileCommonId", fileCommonId);
		query.setParameter("sortOrder", sortOrder);
		query.executeUpdate();

		// Make also sure that the thumb file exists
		fileThumbService.generateThumbFile(fileCommonId);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addGalleryFiles(long galleryId, long[] commonFileIds) {
		var orderId = 0L;

		for (var commonFileId : commonFileIds) {
			addGalleryFile(galleryId, commonFileId, orderId);
			orderId++;
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void deleteGalleryFilesByGalleryId(Long galleryId) {
		var query = entityManager.createNativeQuery("DELETE FROM gallery_file " +
													  "WHERE gallery_id = :galleryId");
		query.setParameter("galleryId", galleryId);
		query.executeUpdate();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void updateGalleryFiles(Long galleryId, long[] commonFileIds) {
		deleteGalleryFilesByGalleryId(galleryId);
		addGalleryFiles(galleryId, commonFileIds);
	}
}
