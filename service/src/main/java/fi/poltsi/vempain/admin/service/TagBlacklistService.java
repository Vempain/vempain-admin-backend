package fi.poltsi.vempain.admin.service;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import fi.poltsi.vempain.admin.api.request.TagBlacklistRequest;
import fi.poltsi.vempain.admin.api.response.TagBlacklistResponse;
import fi.poltsi.vempain.admin.entity.TagBlacklistEntry;
import fi.poltsi.vempain.admin.repository.TagBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the tags that are never published to the website and hands the publishing a {@link TagBlacklist} snapshot to apply. Blacklisting a tag changes
 * nothing on the site until the resources carrying it are published again.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagBlacklistService {

    private final TagBlacklistRepository tagBlacklistRepository;

    public List<TagBlacklistResponse> findAll() {
        return tagBlacklistRepository.findAllByOrderByTagNameAsc()
            .stream()
            .map(TagBlacklistEntry::toResponse)
            .toList();
    }

    /**
     * @throws ResponseStatusException 400 when the tag is blank, 409 when it is already blacklisted (case-insensitively)
     */
    @Transactional
    public TagBlacklistResponse add(TagBlacklistRequest request, long userId) {
        if (request == null
            || request.getTagName() == null
            || request.getTagName()
                .isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag name is required");
        }
        var tagName = request.getTagName()
            .trim();
        if (tagBlacklistRepository.findByTagNameIgnoreCase(tagName)
            .isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag is already blacklisted: " + tagName);
        }
        var entry = tagBlacklistRepository.save(TagBlacklistEntry.builder()
            .tagName(tagName)
            .createdBy(userId)
            .created(Instant.now())
            .build());
        log.info("Tag '{}' blacklisted by user {}", tagName, userId);
        return entry.toResponse();
    }

    /**
     * @throws ResponseStatusException 404 when no such entry exists
     */
    @Transactional
    public void delete(long entryId) {
        var entry = tagBlacklistRepository.findById(entryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such blacklist entry"));
        tagBlacklistRepository.delete(entry);
        log.info("Tag '{}' removed from the blacklist", entry.getTagName());
    }

    /**
     * The blacklist as it is now, for one publish.
     */
    public TagBlacklist snapshot() {
        return TagBlacklist.of(tagBlacklistRepository.findAll()
            .stream()
            .map(TagBlacklistEntry::getTagName)
            .toList());
    }
}
