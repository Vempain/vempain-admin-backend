package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.file.ScanQueueSchedule;
import org.springframework.data.repository.ListCrudRepository;

public interface ScanQueueScheduleRepository extends ListCrudRepository<ScanQueueSchedule, Long> {
}
