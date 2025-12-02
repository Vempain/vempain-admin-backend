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
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Table(name = "subjects")
public class Subject implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long   id;
	@Basic
	@Column(name = "subject", unique = true, nullable = false)
	private String subjectName;
	@Basic
	@Column(name = "subject_de")
	private String subjectNameDe;
	@Basic
	@Column(name = "subject_en")
	private String subjectNameEn;
	@Basic
	@Column(name = "subject_es")
	private String subjectNameEs;
	@Basic
	@Column(name = "subject_fi")
	private String subjectNameFi;
	@Basic
	@Column(name = "subject_se")
	private String subjectNameSe;
}
