package fi.poltsi.vempain.admin.repository;

import fi.poltsi.vempain.admin.entity.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {
	Optional<ApiToken> findByTokenHash(String tokenHash);

	List<ApiToken> findAllByOrderByCreatedDesc();
}
