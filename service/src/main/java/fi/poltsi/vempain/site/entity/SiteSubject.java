package fi.poltsi.vempain.site.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subject")
public class SiteSubject {
	@Id
	@Column(name = "id")
	protected long    id;
	@Column(name = "subject_id")
	private long   subjectId;
	@Column(name = "subject", nullable = false)
	private   String subject;
	@Column(name = "subject_de", nullable = false)
	private   String subjectDe;
	@Column(name = "subject_en", nullable = false)
	private   String subjectEn;
	@Column(name = "subject_fi", nullable = false)
	private   String subjectFi;
	@Column(name = "subject_se", nullable = false)
	private   String subjectSe;
}
