package fi.poltsi.vempain.admin.repository.file;

import org.springframework.data.repository.ListCrudRepository;

import fi.poltsi.vempain.admin.entity.file.ScanQueueSchedule;

public interface ScanQueueScheduleRepository extends ListCrudRepository<ScanQueueSchedule, Long> {
}
