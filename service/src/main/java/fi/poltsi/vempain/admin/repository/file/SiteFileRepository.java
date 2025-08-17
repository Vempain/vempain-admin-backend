package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.api.FileClassEnum;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
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

	@Query(nativeQuery = true, value = "SELECT DISTINCT s.id FROM site_file s JOIN subject_file sf ON s.id = sf.site_file_id")
	List<Long> findAllSiteFileIdWithSubject();

	// Class-only listing for default view
	Page<SiteFile> findByFileClass(FileClassEnum fileClass, PageRequest pageRequest, Pageable pageable);

	// Text filters + class
	Page<SiteFile> findByFileNameContainingIgnoreCaseAndFileClass(String filename, FileClassEnum fileClass, PageRequest pageRequest, Pageable pageable);

	Page<SiteFile> findByFilePathContainingIgnoreCaseAndFileClass(String filePath, FileClassEnum fileClass, PageRequest pageRequest, Pageable pageable);

	Page<SiteFile> findByMimeTypeContainingIgnoreCaseAndFileClass(String mimeType, FileClassEnum fileClass, PageRequest pageRequest, Pageable pageable);

	// Temporal filters + class
	Page<SiteFile> findByCreatedAfterAndFileClass(Instant created, FileClassEnum fileClass, PageRequest pageRequest, Pageable pageable);

	@Query("SELECT s FROM SiteFile s WHERE s.modified > :since AND s.fileClass = :fileClass")
	Page<SiteFile> findByModifiedAfterAndFileClass(@Param("since") Instant since, @Param("fileClass") FileClassEnum fileClass, PageRequest pageRequest, Pageable pageable);

	// Subject-name search (native) + class (bind enum ordinal to match numeric column)
	@Query(
		value = """
			SELECT s.* FROM site_file s
			JOIN subject_file sf ON s.id = sf.site_file_id
			JOIN subject su ON su.id = sf.subject_id
			WHERE LOWER(su.name) LIKE LOWER(CONCAT('%', :subjectName, '%'))
			  AND s.file_class = :fileClassOrdinal
			""",
		countQuery = """
			SELECT COUNT(*) FROM site_file s
			JOIN subject_file sf ON s.id = sf.site_file_id
			JOIN subject su ON su.id = sf.subject_id
			WHERE LOWER(su.name) LIKE LOWER(CONCAT('%', :subjectName, '%'))
			  AND s.file_class = :fileClassOrdinal
			""",
		nativeQuery = true
	)
	Page<SiteFile> findBySubjectNameContainingIgnoreCaseAndFileClass(@Param("subjectName") String subjectName, @Param("fileClassOrdinal") int fileClassOrdinal, PageRequest pageRequest, Pageable pageable);

	// Size filter + class
	Page<SiteFile> findBySizeGreaterThanEqualAndFileClass(Long size, FileClassEnum fileClass, PageRequest pageRequest, Pageable pageable);
}
