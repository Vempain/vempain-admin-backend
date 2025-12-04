package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteFileRepository extends ListPagingAndSortingRepository<SiteFile, Long>, JpaRepository<SiteFile, Long> {
	Optional<SiteFile> findByFilePathAndFileName(String filePath, String fileName);

	List<SiteFile> findByIdIn(ArrayList<Long> siteFileIdList);

	@Query(nativeQuery = true, value = "SELECT DISTINCT s.id FROM site_file s JOIN file_subject sf ON s.id = sf.site_file_id")
	List<Long> findAllSiteFileIdWithSubject();

	// Class-only listing for default view
	Page<SiteFile> findByFileType(FileTypeEnum fileType, PageRequest pageRequest, Pageable pageable);

	// Text filters + class
	Page<SiteFile> findByFileNameContainingIgnoreCaseAndFileType(String filename, FileTypeEnum fileType, PageRequest pageRequest, Pageable pageable);

	Page<SiteFile> findByFilePathContainingIgnoreCaseAndFileType(String filePath, FileTypeEnum fileType, PageRequest pageRequest, Pageable pageable);

	Page<SiteFile> findByMimeTypeContainingIgnoreCaseAndFileType(String mimeType, FileTypeEnum fileType, PageRequest pageRequest, Pageable pageable);

	// Temporal filters + class
	Page<SiteFile> findByCreatedAfterAndFileType(Instant created, FileTypeEnum fileType, PageRequest pageRequest, Pageable pageable);

	@Query("SELECT s FROM SiteFile s WHERE s.modified > :since AND s.fileType = :fileType")
	Page<SiteFile> findByModifiedAfterAndFileType(@Param("since") Instant since, @Param("fileType") FileTypeEnum fileType, PageRequest pageRequest,
												  Pageable pageable);

	// Subject-name search (native) + class (bind enum ordinal to match numeric column)
	@Query(
			value = """
					SELECT s.* FROM site_file s
						JOIN file_subject sf ON s.id = sf.site_file_id
					JOIN subject su ON su.id = sf.subject_id
					WHERE LOWER(su.name) LIKE LOWER(CONCAT('%', :subjectName, '%'))
					  AND s.file_type = :fileType
					""",
			countQuery = """
					SELECT COUNT(*) FROM site_file s
						JOIN file_subject sf ON s.id = sf.site_file_id
					JOIN subject su ON su.id = sf.subject_id
					WHERE LOWER(su.name) LIKE LOWER(CONCAT('%', :subjectName, '%'))
					  AND s.file_type = :fileType
					""",
			nativeQuery = true
	)
	Page<SiteFile> findBySubjectNameContainingIgnoreCaseAndFileType(@Param("subjectName") String subjectName, @Param("fileType") FileTypeEnum fileType,
																	PageRequest pageRequest, Pageable pageable);

	// Size filter + class
	Page<SiteFile> findBySizeGreaterThanEqualAndFileType(Long size, FileTypeEnum fileType, PageRequest pageRequest, Pageable pageable);

	default Optional<SiteFile> findByIdWithoutMetadata(Long id) {
		return findById(id).map(siteFile -> {
			siteFile.setMetadata(null);
			return siteFile;
		});
	}
}
