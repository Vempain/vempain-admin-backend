package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.entity.Unit;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitRepository extends CrudRepository<Unit, Long> {
	void deleteUnitById(long id);
}
