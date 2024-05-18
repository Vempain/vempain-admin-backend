package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.Subject;
import fi.poltsi.vempain.admin.entity.file.FileCommon;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface FileCommonPageableRepository extends PagingAndSortingRepository<FileCommon, Long>,
													  CrudRepository<FileCommon, Long> {
	@Query(value = "SELECT s.* FROM subject s, file_subject fs " +
				   "WHERE fs.file_common_id IN (:fileCommonIdList) " +
				   "AND fs.subject_id = s.id", nativeQuery = true)
	List<Subject> getSubjectsByFileIdList(@Param("fileCommonIdList") long[] fileCommonIdList);

	@Query(value = "SELECT fc.* FROM file_common fc, file_subject fs " +
				   "WHERE fs.subject_id = :subjectId " +
				   "AND fc.id = fs.file_common_id", nativeQuery = true)
	List<FileCommon> getFileCommonBySubjectId(@Param("subjectId") Long subjectId);

	List<FileCommon> findByIdIn(List<Long> idSet);

	@Modifying
	@Query(value = "DELETE FROM file_subject fs WHERE fs.subject_id IN :fileSubjectIdSet", nativeQuery = true)
	void deleteAllBySubjectId(@Param("fileSubjectIdSet") Set<Long> fileSubjectIdSet);

	@Query(value = "SELECT UNIQUE fc.id FROM file_common fc, file_subject fs " +
				   "WHERE fc.id = fs.file_common_id ORDER BY fc.id ASC", nativeQuery =	true)
	List<Long> getAllFileCommonWithSubjects();

	@Query(value = "SELECT CONCAT(fc.siteFilepath, '/', fc.siteFilename) FROM FileCommon fc WHERE fc.id = :id")
	String getFilePathByParentId(Long id);

	Optional<FileCommon> findByConvertedFile(String sourceFile);
}
