package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.file.api.FileTypeEnum;
import fi.poltsi.vempain.site.entity.WebSiteFile;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface WebSiteFileRepository extends PagingAndSortingRepository<WebSiteFile, Long>, JpaRepository<WebSiteFile, Long> {
	@Modifying
	@Transactional
	void deleteByFileId(long fileId);

	@NonNull
	Page<WebSiteFile> findAll(@NonNull Pageable pageable);

	@NonNull
	Page<WebSiteFile> findByFileType(FileTypeEnum fileType, @NonNull Pageable pageable);

	@NonNull
	Page<WebSiteFile> findByFilePathContainingIgnoreCase(String query, @NonNull Pageable pageable);

	@NonNull
	Page<WebSiteFile> findByFileTypeAndFilePathContainingIgnoreCase(FileTypeEnum fileType, String query, @NonNull Pageable pageable);

	// ACL-based filtering variants
	@NonNull
	Page<WebSiteFile> findByAclId(long aclId, @NonNull Pageable pageable);

	@NonNull
	Page<WebSiteFile> findByAclIdAndFileType(long aclId, FileTypeEnum fileType, @NonNull Pageable pageable);

	@NonNull
	Page<WebSiteFile> findByAclIdAndFilePathContainingIgnoreCase(long aclId, String query, @NonNull Pageable pageable);

	@NonNull
	Page<WebSiteFile> findByAclIdAndFileTypeAndFilePathContainingIgnoreCase(long aclId, FileTypeEnum fileType, String query, @NonNull Pageable pageable);
}
