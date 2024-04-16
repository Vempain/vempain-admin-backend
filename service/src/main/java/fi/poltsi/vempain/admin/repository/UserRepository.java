package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
	Optional<User> findByName(String name);

	Optional<User> findByLoginName(String loginName);

	Optional<User> findById(Long id);

	@Modifying
	@Query(value = "UPDATE User SET locked = true WHERE id = :id")
	void lockByUserId(@Param("id") long id);

	@Query("SELECT CASE WHEN MAX(id) IS NULL THEN 1 ELSE (MAX(id) + 1) END AS next FROM User")
	Long getNextAvailableUserId();

	@Modifying
	@Query(value = "UPDATE User SET locked = :locked WHERE id = :userId")
	void updateLockedByUserId(@Param("userId") Long userId, @Param("locked") boolean locked);
}
