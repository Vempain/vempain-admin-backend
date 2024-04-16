package fi.poltsi.vempain.admin.entity;

import jakarta.persistence.Basic;
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

import java.io.Serializable;

@Data
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Table(name = "subject")
public class Subject implements Serializable {
	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	@Column(name = "id")
	private long   id;
	@Basic
	@Column(name = "subject")
	private String subjectName;
	@Basic
	@Column(name = "subject_en")
	private String subjectNameEn;
	@Basic
	@Column(name = "subject_fi")
	private String subjectNameFi;
	@Basic
	@Column(name = "subject_se")
	private String subjectNameSe;
	@Basic
	@Column(name = "subject_de")
	private String subjectNameDe;

}
