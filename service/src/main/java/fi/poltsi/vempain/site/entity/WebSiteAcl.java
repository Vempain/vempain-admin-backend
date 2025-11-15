package fi.poltsi.vempain.site.entity;

import fi.poltsi.vempain.admin.api.site.response.WebSiteAclResponse;
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

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "web_site_acl")
public class WebSiteAcl {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "acl_id", nullable = false)
	private Long aclId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "creator", nullable = false)
	private Long creator;

	@Column(name = "created", nullable = false)
	private Instant created;

	@Column(name = "modifier")
	private Long modifier;

	@Column(name = "modified")
	private Instant modified;

	public WebSiteAclResponse toResponse() {
		return WebSiteAclResponse.builder()
								 .id(this.id)
								 .aclId(this.aclId)
								 .userId(this.userId)
								 .creator(this.creator)
								 .created(this.created)
								 .modifier(this.modifier)
								 .modified(this.modified)
								 .build();
	}
}
