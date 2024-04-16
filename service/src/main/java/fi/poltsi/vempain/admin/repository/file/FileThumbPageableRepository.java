package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.file.FileThumb;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileThumbPageableRepository extends PagingAndSortingRepository<FileThumb, Long>,
													 CrudRepository<FileThumb, Long> {
	@Query(nativeQuery = true, value = "SELECT * FROM file_thumb ft GROUP BY ft.filename , ft.filepath HAVING COUNT(*) > 1")
	Iterable<FileThumb> findAllDuplicates();

	Iterable<FileThumb> findAllByFilepathAndFilename(String filepath, String filename);

	Optional<FileThumb> findFileThumbByParentId(Long parentId);

	@Query(value = "SELECT CONCAT(ft.filepath, '/', ft.filename) FROM FileThumb ft WHERE ft.parentId = :parentId")
	String getFilePathByParentId(Long parentId);
}
