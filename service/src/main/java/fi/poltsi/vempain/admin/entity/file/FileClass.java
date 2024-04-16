package fi.poltsi.vempain.admin.entity.file;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@EqualsAndHashCode
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Table(name = "file_class")
public class FileClass implements Serializable {
	@Id
	@Column(name = "id")
	protected long id;
	@Basic
	@Column(name = "shortname")
	private String shortname;
	@Basic
	@Column(name = "description")
	private String description;

}
