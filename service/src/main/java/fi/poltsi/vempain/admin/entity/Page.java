package fi.poltsi.vempain.admin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.poltsi.vempain.admin.api.response.PageResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@Table(name = "page")
public class Page extends AbstractVempainEntity {
	@Column(name = "parent_id")
	private long    parentId;
	@Column(name = "form_id", nullable = false)
	private long    formId;
	@Column(name = "path", nullable = false)
	private String  path;
	@Column(name = "secure", nullable = false)
	private boolean secure;
	@Column(name = "indexlist", nullable = false)
	private boolean indexList;
	@Column(name = "title", nullable = false, length = 512)
	private String  title;
	@Column(name = "header", nullable = false, length = 512)
	private String  header;
	@Column(name = "body", nullable = false, length = 524288)
	private String  body;

	@Transient
	private Instant published;

	@JsonIgnore
	public PageResponse toResponse() {
		return PageResponse.builder()
						   .id(this.id)
						   .parentId(this.parentId)
						   .formId(this.formId)
						   .path(this.path)
						   .secure(this.secure)
						   .indexList(this.indexList)
						   .title(this.title)
						   .header(this.header)
						   .body(this.body)
						   .acls(null)
						   .creator(this.creator)
						   .created(this.created)
						   .modifier(this.modifier)
						   .modified(this.modified)
						   .published(this.published)
						   .build();
	}
}
