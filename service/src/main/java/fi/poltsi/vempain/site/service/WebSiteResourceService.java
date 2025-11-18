package fi.poltsi.vempain.site.service;

import fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum;
import fi.poltsi.vempain.admin.api.site.response.WebSiteResourcePageResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteResourceResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import fi.poltsi.vempain.site.entity.WebSiteFile;
import fi.poltsi.vempain.site.entity.WebSiteGallery;
import fi.poltsi.vempain.site.entity.WebSitePage;
import fi.poltsi.vempain.site.repository.SiteGalleryRepository;
import fi.poltsi.vempain.site.repository.SitePageRepository;
import fi.poltsi.vempain.site.repository.WebSiteFileRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSiteResourceService {
	private final WebSiteFileRepository siteFileRepository;
	private final SitePageRepository    sitePageRepository;
	private final SiteGalleryRepository siteGalleryRepository;
	private final AccessService         accessService;

	@PersistenceContext(unitName = "site")
	private EntityManager siteEntityManager;

	private static final List<String> ALLOWED_SORT_FIELDS = List.of("id", "path", "title", "shortname", "description", "aclId", "fileType", "created");

	/**
	 * List resources based on the type, file type, and query parameters.
	 *
	 * @param type     the type of the resource (SITE_FILE, GALLERY, PAGE)
	 * @param fileType the file type for filtering site files
	 * @param query    the search query for resource name or path
	 * @param page     the page number (0-based)
	 * @param size     the page size (number of items per page)
	 * @return a paginated response containing the requested resources
	 */
	public WebSiteResourcePageResponse listResources(WebSiteResourceEnum type, FileTypeEnum fileType,
													 String query, Long aclId, String sort, String direction, int page, int size) {
		accessService.checkAuthentication();

		// Defensive normalization for paging
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 200); // cap page size to avoid accidental large scans
		Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;

		// Null type defaults to SITE_FILE
		WebSiteResourceEnum effectiveType = (type == null ? WebSiteResourceEnum.SITE_FILE : type);

		String resolvedSort = resolveSortField(effectiveType, sort);
		Sort sortSpec = Sort.by(dir, resolvedSort);
		PageRequest pageable = PageRequest.of(safePage, safeSize, sortSpec);

		return switch (effectiveType) {
			case GALLERY -> mapGalleryPage(fetchGalleries(query, aclId, pageable));
			case PAGE -> mapPagePage(fetchPages(query, aclId, pageable));
			case SITE_FILE -> mapFilePage(fetchFiles(fileType, query, aclId, pageable));
		};
	}

	private String resolveSortField(WebSiteResourceEnum type, String requested) {
		if (requested == null || requested.isBlank()) {
			return "id";
		}
		String candidate = requested.trim();

		// 'name' is a virtual field accepted for convenience
		if (candidate.equalsIgnoreCase("name")) {
			return switch (type) {
				case SITE_FILE -> "path"; // file path doubles as display name
				case PAGE -> "title";
				case GALLERY -> "shortname";
			};
		}

		// Map snake_case acl_id / file_type to entity field names
		if (candidate.equalsIgnoreCase("acl_id")) {
			candidate = "aclId";
		} else if (candidate.equalsIgnoreCase("file_type")) {
			candidate = "fileType";
		}

		// Validate against whitelist; fallback to id
		if (!ALLOWED_SORT_FIELDS.contains(candidate)) {
			log.warn("Unsupported sort field '{}' for type {}. Falling back to 'id'", candidate, type);
			return "id";
		}
		return candidate;
	}

	private Page<WebSiteFile> fetchFiles(FileTypeEnum fileType, String query, Long aclId, Pageable pageable) {
		// ACL filter precedence
		if (aclId != null) {
			if (fileType != null && query != null && !query.isBlank()) {
				return siteFileRepository.findByAclIdAndFileTypeAndPathContainingIgnoreCase(aclId, fileType, query, pageable);
			} else if (fileType != null) {
				return siteFileRepository.findByAclIdAndFileType(aclId, fileType, pageable);
			} else if (query != null && !query.isBlank()) {
				return siteFileRepository.findByAclIdAndPathContainingIgnoreCase(aclId, query, pageable);
			}
			return siteFileRepository.findByAclId(aclId, pageable);
		}

		if (fileType != null && query != null && !query.isBlank()) {
			return siteFileRepository.findByFileTypeAndPathContainingIgnoreCase(fileType, query, pageable);
		} else if (fileType != null) {
			return siteFileRepository.findByFileType(fileType, pageable);
		} else if (query != null && !query.isBlank()) {
			return siteFileRepository.findByPathContainingIgnoreCase(query, pageable);
		}
		return siteFileRepository.findAll(pageable);
	}

	private Page<WebSitePage> fetchPages(String query, Long aclId, Pageable pageable) {
		List<String> terms = splitTerms(query);
		if (terms.isEmpty()) {
			return aclId != null ? sitePageRepository.findByAclId(aclId, pageable) : sitePageRepository.findAll(pageable);
		}

		LinkedHashMap<Long, WebSitePage> results = new LinkedHashMap<>();
		for (String term : terms) {
			accumulatePage(results, aclId != null ? sitePageRepository.findByAclIdAndTitleContainingIgnoreCase(aclId, term, pageable)
												  : sitePageRepository.findByTitleContainingIgnoreCase(term, pageable));
			accumulatePage(results, aclId != null ? sitePageRepository.findByAclIdAndPathContainingIgnoreCase(aclId, term, pageable)
												  : sitePageRepository.findByPathContainingIgnoreCase(term, pageable));
			accumulatePage(results, aclId != null ? sitePageRepository.findByAclIdAndBodyContainingIgnoreCase(aclId, term, pageable)
												  : sitePageRepository.findByBodyContainingIgnoreCase(term, pageable));
			accumulatePage(results, aclId != null ? sitePageRepository.findByAclIdAndHeaderContainingIgnoreCase(aclId, term, pageable)
												  : sitePageRepository.findByHeaderContainingIgnoreCase(term, pageable));
		}
		return sliceResults(results, pageable);
	}

	private Page<WebSiteGallery> fetchGalleries(String query, Long aclId, Pageable pageable) {
		List<String> terms = splitTerms(query);
		if (terms.isEmpty()) {
			return aclId != null ? siteGalleryRepository.findByAclId(aclId, pageable) : siteGalleryRepository.findAll(pageable);
		}

		LinkedHashMap<Long, WebSiteGallery> results = new LinkedHashMap<>();
		for (String term : terms) {
			accumulateGallery(results, aclId != null ? siteGalleryRepository.findByAclIdAndShortnameContainingIgnoreCase(aclId, term, pageable)
													 : siteGalleryRepository.findByShortnameContainingIgnoreCase(term, pageable));
			accumulateGallery(results, aclId != null ? siteGalleryRepository.findByAclIdAndDescriptionContainingIgnoreCase(aclId, term, pageable)
													 : siteGalleryRepository.findByDescriptionContainingIgnoreCase(term, pageable));
		}
		return sliceResults(results, pageable);
	}

	private List<String> splitTerms(String query) {
		if (query == null) {
			return List.of();
		}
		String trimmed = query.trim();
		if (trimmed.isEmpty()) {
			return List.of();
		}
		Pattern pattern = Pattern.compile("\"([^\"]+)\"|([^\\s]+)");
		Matcher matcher = pattern.matcher(trimmed);
		List<String> tokens = new ArrayList<>();
		while (matcher.find()) {
			String term = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
			if (term != null && !term.isBlank()) {
				tokens.add(term);
			}
		}
		return tokens.isEmpty() ? List.of(trimmed) : tokens;
	}

	private void accumulatePage(LinkedHashMap<Long, WebSitePage> target, Page<WebSitePage> source) {
		if (source == null || source.isEmpty()) {
			return;
		}
		source.forEach(page -> target.putIfAbsent(page.getId(), page));
	}

	private void accumulateGallery(LinkedHashMap<Long, WebSiteGallery> target, Page<WebSiteGallery> source) {
		if (source == null || source.isEmpty()) {
			return;
		}
		source.forEach(gallery -> target.putIfAbsent(gallery.getId(), gallery));
	}

	private <T> Page<T> sliceResults(LinkedHashMap<Long, T> ordered, Pageable pageable) {
		List<T> list = new ArrayList<>(ordered.values());
		long total = list.size();
		int start = (int) Math.min(pageable.getOffset(), total);
		int end = (int) Math.min(start + pageable.getPageSize(), total);
		List<T> content = list.subList(start, end);
		return new PageImpl<>(content, pageable, total);
	}

	private WebSiteResourcePageResponse mapFilePage(Page<WebSiteFile> page) {
		List<WebSiteResourceResponse> items = page.map(file -> WebSiteResourceResponse.builder()
																					  .resourceType(WebSiteResourceEnum.SITE_FILE)
																					  .resourceId(file.getId())
																					  .name(file.getPath())
																					  .path(file.getPath())
																					  .aclId(file.getAclId())
																					  .fileType(file.getFileType() != null ? file.getFileType().shortName : null)
																					  .build())
												  .getContent();
		return toPageResponse(page, items);
	}

	private WebSiteResourcePageResponse mapGalleryPage(Page<WebSiteGallery> page) {
		List<WebSiteResourceResponse> items = page.map(gallery -> WebSiteResourceResponse.builder()
																						 .resourceType(WebSiteResourceEnum.GALLERY)
																						 .resourceId(gallery.getId())
																						 .name(gallery.getShortname())
																						 .path(gallery.getDescription())
																						 .aclId(gallery.getAclId())
																						 .build())
												  .getContent();
		return toPageResponse(page, items);
	}

	private WebSiteResourcePageResponse mapPagePage(Page<WebSitePage> page) {
		List<WebSiteResourceResponse> items = page.map(sitePage -> WebSiteResourceResponse.builder()
																						  .resourceType(WebSiteResourceEnum.PAGE)
																						  .resourceId(sitePage.getId())
																						  .name(sitePage.getTitle())
																						  .path(sitePage.getPath())
																						  .aclId(sitePage.getAclId())
																						  .build())
												  .getContent();
		return toPageResponse(page, items);
	}

	private WebSiteResourcePageResponse toPageResponse(Page<?> page, List<WebSiteResourceResponse> items) {
		return WebSiteResourcePageResponse.builder()
										  .pageNumber(page.getNumber())
										  .pageSize(page.getSize())
										  .totalElements(page.getTotalElements())
										  .totalPages(page.getTotalPages())
										  .items(items)
										  .build();
	}

	public long getNextWebSiteAcl() {
		accessService.checkAuthentication();

		Long maxFileAcl = siteEntityManager.createQuery("SELECT MAX(f.aclId) FROM WebSiteFile f", Long.class)
										   .getSingleResult();
		Long maxGalleryAcl = siteEntityManager.createQuery("SELECT MAX(g.aclId) FROM WebSiteGallery g", Long.class)
											  .getSingleResult();
		Long maxPageAcl = siteEntityManager.createQuery("SELECT MAX(p.aclId) FROM WebSitePage p", Long.class)
										   .getSingleResult();
		log.debug("Max ACLs - File: {}, Gallery: {}, Page: {}", maxFileAcl, maxGalleryAcl, maxPageAcl);

		Long maxAcl = Stream.of(maxFileAcl, maxGalleryAcl, maxPageAcl)
							.filter(java.util.Objects::nonNull)
							.max(Long::compareTo)
							.orElse(0L);
		return maxAcl == 0L ? 1L : maxAcl + 1;
	}
}
