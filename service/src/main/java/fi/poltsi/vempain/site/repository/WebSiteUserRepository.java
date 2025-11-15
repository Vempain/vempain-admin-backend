package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.site.entity.WebSiteUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebSiteUserRepository extends CrudRepository<WebSiteUser, Long> {
	/**
	 * Find a web site user by username
	 *
	 * @param username The username to search for
	 * @return Optional containing the user if found
	 */
	Optional<WebSiteUser> findByUsername(String username);

	/**
	 * Check if a username already exists
	 *
	 * @param username The username to check
	 * @return true if username exists, false otherwise
	 */
	boolean existsByUsername(String username);
}

