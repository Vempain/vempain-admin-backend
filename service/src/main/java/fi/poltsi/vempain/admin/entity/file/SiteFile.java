package fi.poltsi.vempain.admin.entity.file;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "site_file", indexes = {
	@Index(name = "ux_site_file_path_name", columnList = "file_path, file_name", unique = true)
})
public class SiteFile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "file_path", nullable = false, length = 2048)
	private String filePath;

	@Column(name = "mime_type", nullable = false, length = 255)
	private String mimeType;

	@Column(name = "size", nullable = false)
	private long size;

	@Column(name = "creator", length = 255)
	private String creator;

	@Column(name = "created")
	private Instant created;

	@Column(name = "updater", length = 255)
	private String updater;

	@Column(name = "updated")
	private Instant updated;
}

