package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.file.FileVideo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileVideoPageableRepository extends PagingAndSortingRepository<FileVideo, Long>, ListCrudRepository<FileVideo, Long> {
	Page<FileVideo> findByLengthContaining(long filter, PageRequest pageRequest);
	Optional<FileVideo> findFileVideoByParentId(long parentId);
}
