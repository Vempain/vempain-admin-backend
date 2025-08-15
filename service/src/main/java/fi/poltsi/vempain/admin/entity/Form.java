package fi.poltsi.vempain.admin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.poltsi.vempain.admin.api.response.FormResponse;
import fi.poltsi.vempain.auth.entity.AbstractVempainEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Table(name = "form")
public class Form extends AbstractVempainEntity implements Serializable {
	@Basic
	@Column(name = "form_name")
	@NotBlank
	@Size(min = 1, max = 255)
	private String formName;
	@Basic
	@Column(name = "layout_id")
	@NotNull
	private long   layoutId;

	@Transient
	private List<Component> components;

	@JsonIgnore // We do a deep copy of Form by Jackson, so we need this annotation here
	public FormResponse getFormResponse() {
		return FormResponse.builder()
						   .id(this.id)
						   .layoutId(this.layoutId)
						   .name(this.formName)
						   .locked(this.locked)
						   .acls(null)
						   .creator(this.creator)
						   .created(this.created)
						   .modifier(this.modifier)
						   .modified(this.modified)
						   .build();
	}
}
