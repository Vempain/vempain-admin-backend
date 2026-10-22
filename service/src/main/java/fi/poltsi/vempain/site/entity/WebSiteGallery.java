package fi.poltsi.vempain.site.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "web_site_gallery")
public class WebSiteGallery {

    @Column(name = "acl_id", nullable = false)
    protected long aclId;
    /**
     * Nick of the admin user who created the gallery (null for rows published before V1005, until republished).
     */
    @Column(name = "creator", length = 512)
    protected String creator;
    @Column(name = "created", nullable = false)
    protected Instant created;
    /**
     * Nick of the admin user who last modified the gallery.
     */
    @Column(name = "modifier", length = 512)
    protected String modifier;
    @Column(name = "modified")
    protected Instant modified;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;
    @Column(name = "gallery_id", nullable = false)
    private long galleryId;
    @Size(min = 2, max = 2000, message = "Description should be between 2 and 2000 characters")
    @Column(name = "description")
    private String description;
    @Size(min = 2, max = 256, message = "Description should be between 2 and 256 characters")
    @Column(name = "shortname")
    private String shortname;
}
