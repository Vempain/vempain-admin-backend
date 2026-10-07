package fi.poltsi.vempain.admin.repository.file;

import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import fi.poltsi.vempain.tools.LikePatterns;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Collection;

/**
 * Reusable JPA specifications for {@link SiteFile} listings. They are combined with {@code AccessService.readableSpecification()} so
 * that filtering, paging and ACL evaluation all happen in one query.
 */
public final class SiteFileSpecifications {
	private SiteFileSpecifications() {
	}

	public static Specification<SiteFile> hasFileType(FileTypeEnum fileType) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("fileType"), fileType);
	}

	public static Specification<SiteFile> fileNameContains(String text) {
		return containsIgnoreCase("fileName", text);
	}

	public static Specification<SiteFile> filePathContains(String text) {
		return containsIgnoreCase("filePath", text);
	}

	public static Specification<SiteFile> mimeTypeContains(String text) {
		return containsIgnoreCase("mimeType", text);
	}

	public static Specification<SiteFile> createdAfter(Instant instant) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get("created"), instant);
	}

	public static Specification<SiteFile> modifiedAfter(Instant instant) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get("modified"), instant);
	}

	public static Specification<SiteFile> sizeAtLeast(long size) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("size"), size);
	}

	public static Specification<SiteFile> idIn(Collection<Long> ids) {
		return (root, query, criteriaBuilder) -> ids == null || ids.isEmpty()
												 ? criteriaBuilder.disjunction()
												 : root.get("id")
													   .in(ids);
	}

	private static Specification<SiteFile> containsIgnoreCase(String attribute, String text) {
		var pattern = LikePatterns.containsIgnoreCase(text);
		return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get(attribute)), pattern, LikePatterns.ESCAPE_CHAR);
	}
}
