package fi.poltsi.vempain.admin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.poltsi.vempain.admin.api.response.ComponentResponse;
import fi.poltsi.vempain.auth.entity.AbstractVempainEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Table(name = "component")
public class Component extends AbstractVempainEntity implements Serializable {
	@Basic
	@Column(name = "comp_name")
	private String compName;
	@Basic
	@Column(name = "comp_data", length = 524288)
	private String compData;

	@JsonIgnore // We do a deep copy of Component by Jackson, so we need this annotation here
	public ComponentResponse getComponentResponse() {
		return ComponentResponse.builder()
								.id(this.id)
								.compName(this.compName)
								.compData(this.compData)
								.acls(null)
								.locked(this.locked)
								.creator(this.creator)
								.created(this.created)
								.modifier(this.modifier)
								.modified(this.modified)
								.build();
	}
}
