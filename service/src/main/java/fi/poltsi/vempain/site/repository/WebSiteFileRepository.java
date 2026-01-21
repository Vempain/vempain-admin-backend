package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.file.api.FileTypeEnum;
import fi.poltsi.vempain.site.entity.WebSiteFile;
import jakarta.validation.constraints.NotNull;
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

	Page<WebSiteFile> findAll(Pageable pageable);

	@NotNull
	Page<WebSiteFile> findByFileType(FileTypeEnum fileType, @NotNull Pageable pageable);

	@NotNull
	Page<WebSiteFile> findByFilePathContainingIgnoreCase(String query, @NotNull Pageable pageable);

	@NotNull
	Page<WebSiteFile> findByFileTypeAndFilePathContainingIgnoreCase(FileTypeEnum fileType, String query, @NotNull Pageable pageable);

	// ACL-based filtering variants
	@NotNull
	Page<WebSiteFile> findByAclId(long aclId, @NotNull Pageable pageable);

	@NotNull
	Page<WebSiteFile> findByAclIdAndFileType(long aclId, FileTypeEnum fileType, @NotNull Pageable pageable);

	@NotNull
	Page<WebSiteFile> findByAclIdAndFilePathContainingIgnoreCase(long aclId, String query, @NotNull Pageable pageable);

	@NotNull
	Page<WebSiteFile> findByAclIdAndFileTypeAndFilePathContainingIgnoreCase(long aclId, FileTypeEnum fileType, String query, @NotNull Pageable pageable);
}
