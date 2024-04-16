package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.entity.Unit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitRepository extends CrudRepository<Unit, Long> {
	void deleteUnitById(long id);

	@Query("SELECT CASE WHEN MAX(id) IS NULL THEN 1 ELSE (MAX(id) + 1) END AS next FROM Unit")
	Long getNextAvailableUnitId();
}
