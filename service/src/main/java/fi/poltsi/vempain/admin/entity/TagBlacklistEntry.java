package fi.poltsi.vempain.admin.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import fi.poltsi.vempain.admin.api.response.TagBlacklistResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A tag that is removed from everything published to the website. Configuration, not a resource: it carries no ACL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tag_blacklist")
public class TagBlacklistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "tag_name", nullable = false)
    private String tagName;
    @Column(name = "created_by", nullable = false)
    private long createdBy;
    @Column(name = "created", nullable = false)
    private Instant created;

    public TagBlacklistResponse toResponse() {
        return TagBlacklistResponse.builder()
            .id(id)
            .tagName(tagName)
            .createdBy(createdBy)
            .created(created)
            .build();
    }
}
