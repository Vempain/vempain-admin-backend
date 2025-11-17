package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.file.api.FileTypeEnum;
import fi.poltsi.vempain.site.entity.WebSiteFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface WebSiteFileRepository extends PagingAndSortingRepository<WebSiteFile, Long>, JpaRepository<WebSiteFile, Long> {
	@Modifying
	@Transactional
	void deleteByFileId(long fileId);

	Page<WebSiteFile> findAll(Pageable pageable);

	@NonNull
	Page<WebSiteFile> findByFileType(FileTypeEnum fileType, @NonNull Pageable pageable);

	@NonNull
	Page<WebSiteFile> findByPathContainingIgnoreCase(String query, @NonNull Pageable pageable);

	@NonNull
	Page<WebSiteFile> findByFileTypeAndPathContainingIgnoreCase(FileTypeEnum fileType, String query, @NonNull Pageable pageable);

	// ACL-based filtering variants
	@NonNull
	Page<WebSiteFile> findByAclId(long aclId, @NonNull Pageable pageable);

	@NonNull
	Page<WebSiteFile> findByAclIdAndFileType(long aclId, FileTypeEnum fileType, @NonNull Pageable pageable);

	@NonNull
	Page<WebSiteFile> findByAclIdAndPathContainingIgnoreCase(long aclId, String query, @NonNull Pageable pageable);

	@NonNull
	Page<WebSiteFile> findByAclIdAndFileTypeAndPathContainingIgnoreCase(long aclId, FileTypeEnum fileType, String query, @NonNull Pageable pageable);
}
