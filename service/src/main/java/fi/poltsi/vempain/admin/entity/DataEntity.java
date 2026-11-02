package fi.poltsi.vempain.admin.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import fi.poltsi.vempain.admin.api.response.DataResponse;
import fi.poltsi.vempain.admin.api.response.DataSummaryResponse;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.entity.AbstractVempainEntity;
import fi.poltsi.vempain.auth.entity.Acl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * A CSV data set managed by the admin backend and released to the site database as a table. Data sets are ACL-bearing resources: the inherited {@code aclId},
 * {@code creator}/{@code created} and {@code modifier}/{@code modified} come from {@link AbstractVempainEntity} ({@code V1011}).
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = "csvData")
@Table(name = "data_store")
public class DataEntity extends AbstractVempainEntity {

    @Basic
    @Column(name = "identifier", nullable = false, unique = true)
    private String identifier;

    @Basic
    @Column(name = "type", nullable = false)
    private String type;

    @Basic
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Basic
    @Column(name = "column_definitions", nullable = false, columnDefinition = "TEXT")
    private String columnDefinitions;

    @Basic
    @Column(name = "create_sql", nullable = false, columnDefinition = "TEXT")
    private String createSql;

    @Basic
    @Column(name = "fetch_all_sql", nullable = false, columnDefinition = "TEXT")
    private String fetchAllSql;

    @Basic
    @Column(name = "fetch_subset_sql", nullable = false, columnDefinition = "TEXT")
    private String fetchSubsetSql;

    @Basic
    @Column(name = "generated", nullable = false)
    private Instant generated;

    @Basic
    @Column(name = "csv_data", nullable = false, columnDefinition = "TEXT")
    private String csvData;

    /**
     * When the data set was last released to the website (its site table created and filled), null when it has never been published.
     */
    @Basic
    @Column(name = "published")
    private Instant published;

    /** The rows of the data set's ACL, attached by the service for the responses */
    @Transient
    private List<Acl> acls;

    public DataResponse toDataResponse() {
        return DataResponse.builder()
            .id(this.id)
            .aclId(this.aclId)
            .acls(aclResponses())
            .locked(this.locked)
            .creator(this.creator)
            .created(this.created)
            .modifier(this.modifier)
            .modified(this.modified)
            .identifier(this.identifier)
            .type(this.type)
            .description(this.description)
            .columnDefinitions(this.columnDefinitions)
            .createSql(this.createSql)
            .fetchAllSql(this.fetchAllSql)
            .fetchSubsetSql(this.fetchSubsetSql)
            .generated(this.generated)
            .csvData(this.csvData)
            .published(this.published)
            .build();
    }

    public DataSummaryResponse toDataSummaryResponse() {
        return DataSummaryResponse.builder()
            .id(this.id)
            .aclId(this.aclId)
            .acls(aclResponses())
            .locked(this.locked)
            .creator(this.creator)
            .created(this.created)
            .modifier(this.modifier)
            .modified(this.modified)
            .identifier(this.identifier)
            .type(this.type)
            .description(this.description)
            .columnDefinitions(this.columnDefinitions)
            .createSql(this.createSql)
            .fetchAllSql(this.fetchAllSql)
            .fetchSubsetSql(this.fetchSubsetSql)
            .generated(this.generated)
            .published(this.published)
            .build();
    }

    private List<AclResponse> aclResponses() {
        var responses = new ArrayList<AclResponse>();
        if (this.acls != null) {
            for (var acl : this.acls) {
                responses.add(acl.toResponse());
            }
        }
        return responses;
    }
}
