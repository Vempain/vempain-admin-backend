package fi.poltsi.vempain.admin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.poltsi.vempain.admin.api.response.AclResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

@Data
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"permissionId"})
@ToString
@Table(name = "acl")
public class Acl implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "permission_id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long permissionId;

	@Column(name = "acl_id", nullable = false)
	private long aclId;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "unit_id")
	private Long unitId;

	@Column(name = "create_privilege", nullable = false)
	private boolean createPrivilege;

	@Column(name = "read_privilege", nullable = false)
	private boolean readPrivilege;

	@Column(name = "modify_privilege", nullable = false)
	private boolean modifyPrivilege;

	@Column(name = "delete_privilege", nullable = false)
	private boolean deletePrivilege;

	@JsonIgnore
	public AclResponse toResponse() {
		return AclResponse.builder()
						  .permissionId(this.permissionId)
						  .aclId(this.aclId)
						  .user(this.userId)
						  .unit(this.unitId)
						  .createPrivilege(this.createPrivilege)
						  .readPrivilege(this.readPrivilege)
						  .modifyPrivilege(this.modifyPrivilege)
						  .deletePrivilege(this.deletePrivilege)
						  .build();
	}
}
