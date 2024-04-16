package fi.poltsi.vempain.admin.entity.file;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AbstractFileEntity {
	@Id
	@Column(name = "id")
	protected long   id;
	@Basic
	@Column(name = "parent_id")
	private   long   parentId;

	@Transient
	private FileCommon fileCommon;
}
