package fi.poltsi.vempain.site.entity;

import java.time.Instant;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "web_site_subject")
public class WebSiteSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;
    @Basic
    @Column(name = "subject")
    private String subjectName;
    @Basic
    @Column(name = "subject_de")
    private String subjectNameDe;
    @Basic
    @Column(name = "subject_en")
    private String subjectNameEn;
    @Basic
    @Column(name = "subject_es")
    private String subjectNameEs;
    @Basic
    @Column(name = "subject_fi")
    private String subjectNameFi;
    @Basic
    @Column(name = "subject_se")
    private String subjectNameSe;

    /**
     * Nick of the admin user who created the row on the site side, null for rows published before the audit columns existed.
     */
    @Column(name = "creator", length = 512)
    private String creator;

    @Column(name = "created")
    private Instant created;

    /**
     * Nick of the admin user who last changed the row on the site side.
     */
    @Column(name = "modifier", length = 512)
    private String modifier;

    @Column(name = "modified")
    private Instant modified;
}
