package fi.poltsi.vempain.admin.entity.file;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AbstractFileImageEntity extends AbstractFileEntity implements Serializable {
	@Basic
	@Column(name = "width")
	private   long   width;
	@Basic
	@Column(name = "height")
	private   long   height;
}
