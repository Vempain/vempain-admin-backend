package fi.poltsi.vempain.admin.entity;

import fi.poltsi.vempain.admin.api.response.LayoutResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Table(name = "layout")
public class Layout extends AbstractVempainEntity {
	@Column(name = "layout_name")
	private String layoutName;
	@Column(name = "structure", length = 524288)
	private String structure;

	public LayoutResponse getLayoutResponse() {
		return LayoutResponse.builder()
							 .id(this.id)
							 .layoutName(this.layoutName)
							 .structure(this.structure)
							 .acls(null)
							 .locked(this.locked)
							 .creator(this.creator)
							 .created(this.created)
							 .modifier(this.modifier)
							 .modified(this.modified)
							 .build();
	}
}
