package fi.poltsi.vempain.site.repository;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import fi.poltsi.vempain.common.api.FileTypeEnum;
import fi.poltsi.vempain.site.entity.WebSiteFile;

@Repository
public interface WebSiteFileRepository extends PagingAndSortingRepository<WebSiteFile, Long>, JpaRepository<WebSiteFile, Long> {

    /**
     * The site rows that stand for an admin site file: by the admin file id, and also by the file and thumbnail paths of the site, which are unique on the site
     * ({@code ux_web_site_file_thumbnail_path}) and may be held by a row of a file that was ingested again under a new admin id. A republish updates the first
     * one in place and removes the others.
     */
    List<WebSiteFile> findByFileIdOrFilePathOrThumbnailPathOrderByIdAsc(long fileId, String filePath, String thumbnailPath);

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
