package fi.poltsi.vempain.admin.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import fi.poltsi.vempain.admin.api.PublishStatusEnum;
import fi.poltsi.vempain.admin.entity.PublishSchedule;

@Repository
public interface PublishScheduleRepository extends ListCrudRepository<PublishSchedule, Long> {

    @Query("SELECT ps FROM PublishSchedule ps WHERE ps.publishTime > CURRENT_TIMESTAMP AND ps.publishStatus = ?1 ORDER BY ps.publishTime ASC")
    List<PublishSchedule> findAllUpcomingSchedulesByPublishStatus(PublishStatusEnum PublishStatusEnum);

    List<PublishSchedule> findAllByPublishStatusEqualsAndPublishTimeBefore(PublishStatusEnum publishStatusEnum, Instant publishTime);

    /**
     * Number of schedules in the given status; the pending publishes gauge reads NOT_PUBLISHED.
     */
    long countByPublishStatus(PublishStatusEnum publishStatus);

    @Transactional
    @Modifying
    @Query("UPDATE PublishSchedule ps SET ps.publishStatus = ?2 WHERE ps.id = ?1")
    void updatePublishStatus(long publishId, PublishStatusEnum publishStatusEnum);
}
