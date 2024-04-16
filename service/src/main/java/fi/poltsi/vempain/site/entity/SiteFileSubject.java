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
@Table(name = "file_subject")
public class SiteFileSubject {
	@Id
	@Column(name = "file_common_id")
	protected long    fileCommonId;
	@Column(name = "subject_id")
	protected long    subjectId;
}
