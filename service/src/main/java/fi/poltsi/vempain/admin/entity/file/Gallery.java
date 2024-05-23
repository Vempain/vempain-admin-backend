package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.admin.api.response.AclResponse;
import fi.poltsi.vempain.admin.api.response.file.FileCommonResponse;
import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
import fi.poltsi.vempain.admin.entity.AbstractVempainEntity;
import fi.poltsi.vempain.admin.entity.Acl;
import fi.poltsi.vempain.site.entity.SiteGallery;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Table(name = "gallery")
public class Gallery extends AbstractVempainEntity implements Serializable {
	@Basic
	@Size(min = 2, max = 2000, message = "Description should be between 2 and 2000 characters")
	@Column(name = "description")
	private String description;
	@Basic
	@Size(min = 2, max = 256, message = "Description should be between 2 and 256 characters")
	@Column(name = "shortname")
	private String shortname;

	@Transient
	private List<FileCommon> commonFiles;
	@Transient
	private List<Acl>        acls;

	public GalleryResponse getResponse() {
		var commonFileResponses = new ArrayList<FileCommonResponse>();

		if (this.commonFiles != null) {
			for (var commonFile : this.commonFiles) {
				commonFileResponses.add(commonFile.toResponse());
			}
		}

		var aclResponses = new ArrayList<AclResponse>();
		if (this.acls != null) {
			for (var acl : this.acls) {
				aclResponses.add(acl.toResponse());
			}
		}
		return GalleryResponse.builder()
							  .acls(aclResponses)
							  .id(this.id)
							  .shortName(this.shortname)
							  .description(this.description)
							  .commonFiles(commonFileResponses)
							  .build();
	}

	public SiteGallery getSiteGallery() {
		return SiteGallery.builder()
						  .galleryId(this.id)
						  .description(this.description)
						  .shortname(this.shortname)
						  .creator(this.creator)
						  .created(this.created)
						  .modifier(this.modifier)
						  .modified(this.modified)
						  .build();
	}
}
