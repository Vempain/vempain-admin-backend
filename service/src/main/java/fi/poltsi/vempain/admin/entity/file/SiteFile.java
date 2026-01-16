package fi.poltsi.vempain.admin.entity.file;

import fi.poltsi.vempain.admin.api.response.file.SiteFileResponse;
import fi.poltsi.vempain.auth.entity.AbstractVempainEntity;
import fi.poltsi.vempain.common.DurationToLongConverter;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import fi.poltsi.vempain.site.entity.WebSiteFile;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.File;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Getter
@Setter
@Entity
@ToString(callSuper = true, exclude = "metadata")
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "site_file", indexes = {@Index(name = "ux_site_file_path_name", columnList = "file_path, file_name", unique = true)})
public class SiteFile extends AbstractVempainEntity implements Serializable {

	@Column(name = "file_id", nullable = false)
	private long fileId;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "file_path", nullable = false, length = 2048)
	private String filePath;

	@Column(name = "mime_type", nullable = false, length = 255)
	private String mimeType;

	@Column(name = "size", nullable = false)
	private long size;

	@Enumerated(EnumType.STRING)
	@Column(name = "file_type", nullable = false)
	private FileTypeEnum fileType;

	@Column(name = "comment", nullable = false)
	private String comment;

	@Column(name = "metadata", nullable = false)
	private String metadata;

	@Column(name = "width")
	private Integer width;
	@Column(name = "height")
	private Integer height;

	@Convert(converter = DurationToLongConverter.class)
	@Column(name = "length")
	private Duration length;

	@Column(name = "pages")
	private Integer pages;

	@Column(name = "sha256sum", nullable = false)
	private String sha256sum;

	@Column(name = "original_datetime")
	private Instant originalDateTime;

	@Column(name = "rights_holder")
	private String rightsHolder;

	@Column(name = "rights_terms")
	private String rightsTerms;

	@Column(name = "rights_url", length = 1024)
	private String rightsUrl;

	@Column(name = "creator_name")
	private String creatorName;

	@Column(name = "creator_email")
	private String creatorEmail;

	@Column(name = "creator_country")
	private String creatorCountry;

	@Column(name = "creator_url", length = 1024)
	private String creatorUrl;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "location_id")
	private GpsLocation location;

	public SiteFileResponse toResponse() {
		return SiteFileResponse.builder()
							   .id(this.id)
							   .fileId(this.fileId)
							   .fileName(this.fileName)
							   .filePath(this.filePath)
							   .fileType(this.fileType)
							   .mimeType(this.mimeType)
							   .size(this.size)
							   .sha256sum(this.sha256sum)
							   .creator(this.creator)
							   .created(this.created)
							   .modifier(this.modifier)
							   .modified(this.modified)
							   .comment(this.comment)
							   .width(this.width)
							   .height(this.height)
							   .length(this.length)
							   .pages(this.pages)
							   .metadata(this.metadata)
							   .originalDateTime(this.originalDateTime)
							   .rightsHolder(this.rightsHolder)
							   .rightsTerms(this.rightsTerms)
							   .rightsUrl(this.rightsUrl)
							   .creatorName(this.creatorName)
							   .creatorEmail(this.creatorEmail)
							   .creatorCountry(this.creatorCountry)
							   .creatorUrl(this.creatorUrl)
							   .location(this.location != null ? this.location.toResponse() : null)
							   .build();
	}

	public WebSiteFile toWebSiteFile() {
		return WebSiteFile.builder()
						  .fileId(this.id)
						  .aclId(this.aclId)
						  .comment(this.comment)
						  .path(this.filePath + File.pathSeparator + this.fileName)
						  .mimetype(this.mimeType)
						  .fileType(this.fileType)
						  .originalDateTime(this.originalDateTime)
						  .rightsHolder(this.rightsHolder)
						  .rightsTerms(this.rightsTerms)
						  .rightsUrl(this.rightsUrl)
						  .creatorName(this.creatorName)
						  .creatorEmail(this.creatorEmail)
						  .creatorCountry(this.creatorCountry)
						  .creatorUrl(this.creatorUrl)
						  .location(this.location != null ? this.location.toWebGpsLocation() : null)
						  .width(this.width != null ? this.width : 0)
						  .height(this.height != null ? this.height : 0)
						  .length(this.length)
						  .pages(this.pages)
						  .metadata(this.metadata)
						  .build();
	}
}
