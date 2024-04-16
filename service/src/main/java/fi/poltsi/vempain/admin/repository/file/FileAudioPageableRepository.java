package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.file.FileAudio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileAudioPageableRepository extends PagingAndSortingRepository<FileAudio, Long>, ListCrudRepository<FileAudio, Long> {
	Page<FileAudio> findByLengthContaining(long filter, PageRequest pageRequest);
}
