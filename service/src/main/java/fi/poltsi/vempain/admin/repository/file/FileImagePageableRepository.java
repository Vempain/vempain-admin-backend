package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.file.FileImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileImagePageableRepository extends PagingAndSortingRepository<FileImage, Long>, ListCrudRepository<FileImage, Long> {

	@Query(value = "SELECT fi.id, fi.parentId, fi.height, fi.height " +
				   "FROM FileImage fi, FileCommon fc " +
				   "WHERE fc.siteFilepath = :siteFilePath " +
				   "AND fc.siteFilename = :siteFilename " +
				   "AND fc.id = fi.parentId")
	Iterable<FileImage> findAllFileImageByFileCommonPathAndName(String siteFilePath, String siteFilename);

	Optional<FileImage> findImageFileByParentId(long id);

	@Query(value = "SELECT fi.id, fi.parent_id, fi.height, fi.width " +
				   "FROM file_image fi  " +
				   "WHERE fi.parent_id NOT IN (SELECT parent_id FROM file_thumb)", nativeQuery = true)
	Iterable<FileImage> findAllFileImageWithoutThumbnail();

	Page<FileImage> findByHeightContaining(long filter, PageRequest pageRequest);

	Page<FileImage> findByWidthContaining(long filter, PageRequest pageRequest);

	Page<FileImage> findByParentIdContaining(long filter, PageRequest pageRequest);
}
