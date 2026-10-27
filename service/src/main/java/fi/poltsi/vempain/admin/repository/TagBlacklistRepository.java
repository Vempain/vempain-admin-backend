package fi.poltsi.vempain.admin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fi.poltsi.vempain.admin.entity.TagBlacklistEntry;

@Repository
public interface TagBlacklistRepository extends JpaRepository<TagBlacklistEntry, Long> {

    List<TagBlacklistEntry> findAllByOrderByTagNameAsc();

    Optional<TagBlacklistEntry> findByTagNameIgnoreCase(String tagName);
}
