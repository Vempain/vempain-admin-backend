package fi.poltsi.vempain.site.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import fi.poltsi.vempain.admin.api.site.WebSiteConfigurationTypeEnum;
import fi.poltsi.vempain.admin.api.site.response.WebSiteConfigurationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "web_site_configuration")
public class WebSiteConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "config_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private WebSiteConfigurationTypeEnum configType;

    @Column(name = "config_default", nullable = false, length = 4000)
    private String configDefault;

    @Column(name = "config_value", length = 4000, nullable = false)
    private String configValue;

    /**
     * Nick of the admin user who created the row, null for the rows seeded by the migrations.
     */
    @Column(name = "creator", length = 512)
    private String creator;

    @Column(name = "created")
    private Instant created;

    /**
     * Nick of the admin user who last updated the value.
     */
    @Column(name = "modifier", length = 512)
    private String modifier;

    @Column(name = "modified")
    private Instant modified;

    public WebSiteConfigurationResponse toResponse() {
        return WebSiteConfigurationResponse.builder()
            .id(this.id)
            .configKey(this.configKey)
            .configType(this.configType)
            .configDefault(this.configDefault)
            .configValue(this.configValue)
            .creator(this.creator)
            .created(this.created)
            .modifier(this.modifier)
            .modified(this.modified)
            .build();
    }
}
