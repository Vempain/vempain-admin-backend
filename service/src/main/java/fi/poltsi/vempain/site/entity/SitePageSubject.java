package fi.poltsi.vempain.site.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

public class SitePageSubject {
	@Id
	@Column(name = "page_id")
	protected long    pageId;
	@Column(name = "subject_id")
	protected long    subjectId;
}
