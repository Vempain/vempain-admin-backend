package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.api.ContentTypeEnum;
import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import fi.poltsi.vempain.admin.entity.PublishSchedule;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PublishScheduleRepository extends ListCrudRepository<PublishSchedule, Long> {
	@Query("SELECT ps FROM PublishSchedule ps WHERE ps.publishTime < CURRENT_TIMESTAMP AND ps.publishStatus = ?1 ORDER BY ps.publishTime ASC")
	List<PublishSchedule> findAllUpcomingSchedulesByPublishStatus(PublishStatusEnum PublishStatusEnum);

	@Transactional
	@Modifying
	@Query("UPDATE PublishSchedule ps SET ps.publishStatus = 'PUBLISHED' WHERE ps.id = ?1 AND ps.publishType = ?2")
	void updatePublishStatus(long publishId, ContentTypeEnum contentTypeEnum);
}
