package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.admin.api.response.AclResponse;
import fi.poltsi.vempain.admin.api.response.file.FileCommonResponse;
import fi.poltsi.vempain.admin.entity.AbstractVempainEntity;
import fi.poltsi.vempain.admin.entity.Acl;
import jakarta.persistence.Basic;
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

import java.awt.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Table(name = "file_common")
public class FileCommon extends AbstractVempainEntity implements Serializable {
	@Basic
	@Column(name = "mimetype")
	private   String  mimetype;
	@Column(name = "file_class_id")
	private   long    fileClassId;
	@Basic
	@Column(name = "comment", columnDefinition = "TEXT")
	private   String  comment;
	@Basic
	@Column(name = "metadata", columnDefinition = "TEXT")
	private   String  metadata;

	// These are the files that the service is provided with from the Vempain File-server, i.e. they're intermediate files that
	// are in processed form (e.g. JPEG, PDF, MKV) and exist in the ${admin.file.converted-directory} directory
	@Basic
	@Column(name = "converted_file", columnDefinition = "TEXT")
	private   String convertedFile;
	@Basic
	@Column(name = "converted_filesize")
	private   Long    convertedFilesize;
	@Basic
	@Column(name = "converted_sha1sum", length = 40)
	private   String  convertedSha1sum;
	@Basic
	@Column(name = "original_datetime")
	protected Instant originalDatetime;
	@Basic
	@Column(name = "original_second_fraction")
	protected Integer originalSecondFraction;
	@Basic
	@Column(name = "original_document_id")
	protected String  originalDocumentId;

	// Site file details, as they exist on the remote site
	@Basic
	@Column(name = "site_filename", columnDefinition = "TEXT")
	private String siteFilename;
	@Basic
	@Column(name = "site_filepath", columnDefinition = "TEXT")
	private String siteFilepath;
	@Basic
	@Column(name = "site_filesize")
	private Long   siteFilesize;
	@Basic
	@Column(name = "site_sha1sum", length = 40)
	private String siteSha1sum;
	@Transient
	private List<Acl> acls;

	// These are temporary fields that we require only to pass the site file details when doing the file transfer
	@Transient
	private Dimension siteFileDimension;

	public FileCommonResponse toResponse() {
		var aclResponses = new ArrayList<AclResponse>();

		if (this.acls != null) {
			aclResponses.addAll(this.acls.stream().map(Acl::toResponse).toList());
		}

		return FileCommonResponse.builder()
								 .id(this.getId())
								 .fileClassId(this.getFileClassId())
								 .comment(this.getComment())
								 .convertedFile(this.getConvertedFile())
								 .convertedFile(this.convertedFile)
								 .convertedFilesize(this.convertedFilesize)
								 .convertedSha1sum(this.convertedSha1sum)
								 .siteFilename(this.siteFilename)
								 .siteFilepath(this.siteFilepath)
								 .siteFilesize(this.siteFilesize)
								 .siteSha1sum(this.siteSha1sum)
								 .originalDateTime(this.originalDatetime)
								 .originalSecondFraction(this.originalSecondFraction)
								 .originalDocumentId(this.originalDocumentId)
								 .acls(aclResponses)
								 .comment(this.comment)
								 .metadata(this.metadata)
								 .mimetype(this.mimetype)
								 .locked(this.isLocked())
								 .creator(this.creator)
								 .created(this.created)
								 .modifier(this.modifier)
								 .modified(this.modified)
								 .build();
	}
}
