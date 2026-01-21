package fi.poltsi.vempain.site.service;

import fi.poltsi.vempain.admin.api.site.WebSiteResourceEnum;
import fi.poltsi.vempain.admin.api.site.response.WebSiteResourcePageResponse;
import fi.poltsi.vempain.admin.api.site.response.WebSiteResourceResponse;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.file.api.FileTypeEnum;
import fi.poltsi.vempain.site.entity.WebSiteFile;
import fi.poltsi.vempain.site.entity.WebSiteGallery;
import fi.poltsi.vempain.site.entity.WebSitePage;
import fi.poltsi.vempain.site.repository.WebSiteFileRepository;
import fi.poltsi.vempain.site.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.site.repository.WebSitePageRepository;
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
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSiteResourceService {
	private final WebSiteFileRepository siteFileRepository;
	private final WebSitePageRepository    webSitePageRepository;
	private final WebSiteGalleryRepository webSiteGalleryRepository;
	private final AccessService            accessService;

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
		var safePage = Math.max(page, 0);
		var safeSize = Math.min(Math.max(size, 1), 200); // cap page size to avoid accidental large scans
		var dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
		var allTypes = (type == null);
		var effectiveType = allTypes ? WebSiteResourceEnum.SITE_FILE : type;

		var resolvedSort = resolveSortField(effectiveType, sort);
		var sortSpec = Sort.by(dir, resolvedSort);
		var pageable = PageRequest.of(safePage, safeSize, sortSpec);

		if (allTypes) {
			return listAllResourceTypes(query, aclId, sortSpec, safePage, safeSize);
		}

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

		var candidate = requested.trim();

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
				return siteFileRepository.findByAclIdAndFileTypeAndFilePathContainingIgnoreCase(aclId, fileType, query, pageable);
			} else if (fileType != null) {
				return siteFileRepository.findByAclIdAndFileType(aclId, fileType, pageable);
			} else if (query != null && !query.isBlank()) {
				return siteFileRepository.findByAclIdAndFilePathContainingIgnoreCase(aclId, query, pageable);
			}
			return siteFileRepository.findByAclId(aclId, pageable);
		}

		if (fileType != null && query != null && !query.isBlank()) {
			return siteFileRepository.findByFileTypeAndFilePathContainingIgnoreCase(fileType, query, pageable);
		} else if (fileType != null) {
			return siteFileRepository.findByFileType(fileType, pageable);
		} else if (query != null && !query.isBlank()) {
			return siteFileRepository.findByFilePathContainingIgnoreCase(query, pageable);
		}

		return siteFileRepository.findAll(pageable);
	}

	private Page<WebSitePage> fetchPages(String query, Long aclId, Pageable pageable) {
		var terms = splitTerms(query);

		if (terms.isEmpty()) {
			return aclId != null ? webSitePageRepository.findByAclId(aclId, pageable) : webSitePageRepository.findAll(pageable);
		}

		var results = new LinkedHashMap<Long, WebSitePage>();

		for (String term : terms) {
			accumulatePage(results, aclId != null ? webSitePageRepository.findByAclIdAndTitleContainingIgnoreCase(aclId, term, pageable)
												  : webSitePageRepository.findByTitleContainingIgnoreCase(term, pageable));
			accumulatePage(results, aclId != null ? webSitePageRepository.findByAclIdAndFilePathContainingIgnoreCase(aclId, term, pageable)
												  : webSitePageRepository.findByFilePathContainingIgnoreCase(term, pageable));
			accumulatePage(results, aclId != null ? webSitePageRepository.findByAclIdAndBodyContainingIgnoreCase(aclId, term, pageable)
												  : webSitePageRepository.findByBodyContainingIgnoreCase(term, pageable));
			accumulatePage(results, aclId != null ? webSitePageRepository.findByAclIdAndHeaderContainingIgnoreCase(aclId, term, pageable)
												  : webSitePageRepository.findByHeaderContainingIgnoreCase(term, pageable));
		}

		return sliceResults(results, pageable);
	}

	private Page<WebSiteGallery> fetchGalleries(String query, Long aclId, Pageable pageable) {
		var terms = splitTerms(query);

		if (terms.isEmpty()) {
			return aclId != null ? webSiteGalleryRepository.findByAclId(aclId, pageable) : webSiteGalleryRepository.findAll(pageable);
		}

		var results = new LinkedHashMap<Long, WebSiteGallery>();

		for (var term : terms) {
			accumulateGallery(results, aclId != null ? webSiteGalleryRepository.findByAclIdAndShortnameContainingIgnoreCase(aclId, term, pageable)
													 : webSiteGalleryRepository.findByShortnameContainingIgnoreCase(term, pageable));
			accumulateGallery(results, aclId != null ? webSiteGalleryRepository.findByAclIdAndDescriptionContainingIgnoreCase(aclId, term, pageable)
													 : webSiteGalleryRepository.findByDescriptionContainingIgnoreCase(term, pageable));
		}
		return sliceResults(results, pageable);
	}

	private List<String> splitTerms(String query) {
		if (query == null) {
			return List.of();
		}

		var trimmed = query.trim();

		if (trimmed.isEmpty()) {
			return List.of();
		}

		var pattern = Pattern.compile("\"([^\"]+)\"|([^\\s]+)");
		var matcher = pattern.matcher(trimmed);
		var tokens = new ArrayList<String>();

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
		var total = list.size();
		var start = (int) Math.min(pageable.getOffset(), total);
		var end = (int) Math.min(start + pageable.getPageSize(), total);
		List<T> content = list.subList(start, end);
		return new PageImpl<>(content, pageable, total);
	}

	private WebSiteResourcePageResponse mapFilePage(Page<WebSiteFile> page) {
		var items = page.map(file -> WebSiteResourceResponse.builder()
																					  .resourceType(WebSiteResourceEnum.SITE_FILE)
																					  .resourceId(file.getId())
															.name(file.getFilePath())
															.path(file.getFilePath())
																					  .aclId(file.getAclId())
																					  .fileType(file.getFileType() != null ? file.getFileType().shortName : null)
																					  .build())
												  .getContent();
		return toPageResponse(page, items);
	}

	private WebSiteResourcePageResponse mapGalleryPage(Page<WebSiteGallery> page) {
		var items = page.map(gallery -> WebSiteResourceResponse.builder()
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
		var items = page.map(sitePage -> WebSiteResourceResponse.builder()
																						  .resourceType(WebSiteResourceEnum.PAGE)
																						  .resourceId(sitePage.getId())
																						  .name(sitePage.getTitle())
																.path(sitePage.getFilePath())
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

		var maxAcl = Stream.of(maxFileAcl, maxGalleryAcl, maxPageAcl)
							.filter(java.util.Objects::nonNull)
							.max(Long::compareTo)
							.orElse(0L);
		return maxAcl == 0L ? 1L : maxAcl + 1;
	}

	private WebSiteResourcePageResponse listAllResourceTypes(String query, Long aclId, Sort sortSpec, int safePage, int safeSize) {
		var requiredLong = (long) (safePage + 1) * safeSize;

		if (requiredLong > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Requested page is too large to materialize");
		}

		var required = (int) requiredLong;
		var multiPageable = PageRequest.of(0, required, sortSpec);

		var filePage = fetchFiles(null, query, aclId, multiPageable);
		var galleryPage = fetchGalleries(query, aclId, multiPageable);
		var pagePage = fetchPages(query, aclId, multiPageable);

		var totalElements = filePage.getTotalElements() + galleryPage.getTotalElements() + pagePage.getTotalElements();

		if (totalElements == 0) {
			return WebSiteResourcePageResponse.builder()
											  .pageNumber(safePage)
											  .pageSize(safeSize)
											  .totalElements(0)
											  .totalPages(0)
											  .items(List.of())
											  .build();
		}

		var combined = new ArrayList<WebSiteResourceResponse>();
		combined.addAll(toFileResponses(filePage));
		combined.addAll(toGalleryResponses(galleryPage));
		combined.addAll(toPageResponses(pagePage));

		var start = (long) safePage * safeSize;

		if (start >= totalElements || start >= combined.size()) {
			return WebSiteResourcePageResponse.builder()
											  .pageNumber(safePage)
											  .pageSize(safeSize)
											  .totalElements(totalElements)
											  .totalPages((int) Math.ceil((double) totalElements / safeSize))
											  .items(List.of())
											  .build();
		}

		var end = (int) Math.min(start + safeSize, Math.min(totalElements, combined.size()));
		var pageItems = combined.subList((int) start, end);
		return WebSiteResourcePageResponse.builder()
										  .pageNumber(safePage)
										  .pageSize(safeSize)
										  .totalElements(totalElements)
										  .totalPages((int) Math.ceil((double) totalElements / safeSize))
										  .items(pageItems)
										  .build();
	}

	private List<WebSiteResourceResponse> toFileResponses(Page<WebSiteFile> filePage) {
		return filePage.map(file -> WebSiteResourceResponse.builder()
														   .resourceType(WebSiteResourceEnum.SITE_FILE)
														   .resourceId(file.getId())
														   .name(file.getFilePath())
														   .path(file.getFilePath())
														   .aclId(file.getAclId())
														   .fileType(file.getFileType() != null ? file.getFileType().shortName : null)
														   .build())
					   .getContent();
	}

	private List<WebSiteResourceResponse> toGalleryResponses(Page<WebSiteGallery> galleryPage) {
		return galleryPage.map(gallery -> WebSiteResourceResponse.builder()
																 .resourceType(WebSiteResourceEnum.GALLERY)
																 .resourceId(gallery.getId())
																 .name(gallery.getShortname())
																 .path(gallery.getDescription())
																 .aclId(gallery.getAclId())
																 .build())
						  .getContent();
	}

	private List<WebSiteResourceResponse> toPageResponses(Page<WebSitePage> pagePage) {
		return pagePage.map(sitePage -> WebSiteResourceResponse.builder()
															   .resourceType(WebSiteResourceEnum.PAGE)
															   .resourceId(sitePage.getId())
															   .name(sitePage.getTitle())
															   .path(sitePage.getFilePath())
															   .aclId(sitePage.getAclId())
															   .build())
					   .getContent();
	}
}
