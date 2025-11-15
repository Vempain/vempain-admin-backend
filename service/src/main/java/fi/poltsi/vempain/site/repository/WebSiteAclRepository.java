package fi.poltsi.vempain.site.repository;

import fi.poltsi.vempain.site.entity.WebSiteAcl;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebSiteAclRepository extends CrudRepository<WebSiteAcl, Long> {
	/**
	 * Find all ACL entries for a specific ACL ID
	 *
	 * @param aclId The ACL ID to search for
	 * @return List of ACL entries
	 */
	List<WebSiteAcl> findByAclId(Long aclId);

	/**
	 * Find all ACL entries for a specific user
	 *
	 * @param userId The user ID to search for
	 * @return List of ACL entries
	 */
	List<WebSiteAcl> findByUserId(Long userId);

	/**
	 * Find a specific ACL entry by ACL ID and user ID
	 *
	 * @param aclId  The ACL ID
	 * @param userId The user ID
	 * @return The ACL entry if found
	 */
	WebSiteAcl findByAclIdAndUserId(Long aclId, Long userId);

	/**
	 * Delete all ACL entries for a specific ACL ID
	 *
	 * @param aclId The ACL ID
	 */
	void deleteByAclId(Long aclId);

	/**
	 * Delete all ACL entries for a specific user
	 *
	 * @param userId The user ID
	 */
	void deleteByUserId(Long userId);

	/**
	 * Get all distinct ACL IDs accessible by a user
	 *
	 * @param userId The user ID
	 * @return List of ACL IDs
	 */
	@Query("SELECT DISTINCT a.aclId FROM WebSiteAcl a WHERE a.userId = :userId")
	List<Long> findAclIdsByUserId(@Param("userId") Long userId);
}

