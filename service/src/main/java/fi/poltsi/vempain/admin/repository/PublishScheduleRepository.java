package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import fi.poltsi.vempain.admin.entity.PublishSchedule;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
public interface PublishScheduleRepository extends ListCrudRepository<PublishSchedule, Long> {
	@Query("SELECT ps FROM PublishSchedule ps WHERE ps.publishTime > CURRENT_TIMESTAMP AND ps.publishStatus = ?1 ORDER BY ps.publishTime ASC")
	List<PublishSchedule> findAllUpcomingSchedulesByPublishStatus(PublishStatusEnum PublishStatusEnum);

	List<PublishSchedule> findAllByPublishStatusEqualsAndPublishTimeBefore(PublishStatusEnum publishStatusEnum, Instant publishTime);

	@Transactional
	@Modifying
	@Query("UPDATE PublishSchedule ps SET ps.publishStatus = ?2 WHERE ps.id = ?1")
	void updatePublishStatus(long publishId, PublishStatusEnum publishStatusEnum);
}
