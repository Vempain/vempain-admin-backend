package fi.poltsi.vempain.site.entity;

import fi.poltsi.vempain.admin.api.site.response.WebSiteResourceResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteUserResponse;
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

import java.time.Instant;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "web_site_users")
public class WebSiteUser {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "username", nullable = false, unique = true, length = 256)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 512)
	private String passwordHash;

	@Column(name = "creator", nullable = false)
	private Long creator;

	@Column(name = "created", nullable = false)
	private Instant created;

	@Column(name = "modifier")
	private Long modifier;

	@Column(name = "modified")
	private Instant modified;

	public WebSiteUserResponse toResponse() {
		return WebSiteUserResponse.builder()
								  .id(this.id)
								  .username(this.username)
								  .creator(this.creator)
								  .created(this.created)
								  .modifier(this.modifier)
								  .modified(this.modified)
								  .resources(null)
								  .build();
	}

	public WebSiteUserResponse toResponse(List<WebSiteResourceResponse> resources) {
		return WebSiteUserResponse.builder()
								  .id(this.id)
								  .username(this.username)
								  .creator(this.creator)
								  .created(this.created)
								  .modifier(this.modifier)
								  .modified(this.modified)
								  .resources(resources)
								  .build();
	}
}
