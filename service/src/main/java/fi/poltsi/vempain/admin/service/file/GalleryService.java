package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.request.file.GalleryRequest;
import fi.poltsi.vempain.admin.api.response.file.FileGroupListResponse;
import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.service.AclService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
@Service
public class GalleryService {
	private final GalleryRepository  galleryRepository;
	private final SiteFileRepository siteFileRepository;
	private final GalleryFileService galleryFileService;
	private final AclService    aclService;
	private final AccessService accessService;

	public List<Gallery> findAllForUser() {
		var galleryList = new ArrayList<Gallery>();
		var fullList = galleryRepository.findAll();

		for (Gallery gallery : fullList) {
			if (accessService.hasReadPermission(gallery.getAclId())) {
				populateGalleryWithSiteFiles(gallery, false);
				galleryList.add(gallery);
			}
		}

		return galleryList;
	}

	@Transactional(readOnly = true)
	public List<GalleryResponse> findAllAsResponsesForUser(QueryDetailEnum queryDetailEnum) {
		var galleries = findAllForUser();
		var responses = new ArrayList<GalleryResponse>();

		for (Gallery gallery : galleries) {
			populateGalleryWithSiteFiles(gallery, queryDetailEnum == QueryDetailEnum.FULL);

			var response = gallery.getResponse();
			responses.add(response);
		}

		return responses;
	}

	@Transactional(readOnly = true)
	public GalleryResponse findById(long galleryId) {
		var gallery = galleryRepository.findById(galleryId)
		                               .orElse(null);

		if (gallery == null) {
			return null;
		}

		if (!accessService.hasReadPermission(gallery.getAclId())) {
			return null;
		}

		populateGalleryWithSiteFiles(gallery, false);
		return gallery.getResponse();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public GalleryResponse createGallery(GalleryRequest galleryRequest) throws VempainAclException {
		var aclId = aclService.getNextAclId();
		var gallery = Gallery.builder()
		                     .shortname(galleryRequest.getShortName())
		                     .description(galleryRequest.getDescription())
		                     .aclId(aclId)
		                     .creator(accessService.getUserId())
		                     .created(Instant.now())
		                     .modifier(null)
		                     .modified(null)
		                     .locked(false)
		                     .build();
		var newGallery = galleryRepository.save(gallery);
		galleryFileService.addGalleryFiles(newGallery.getId(), galleryRequest.getSiteFilesId());

		try {
			aclService.saveAclRequests(aclId, galleryRequest.getAcls());
		} catch (Exception e) {
			log.error("Could not create ACLs for new gallery with ID: {}", newGallery.getId(), e);
			throw new VempainAclException("Could not create ACLs for new gallery with ID: " + newGallery.getId());
		}

		populateGalleryWithSiteFiles(newGallery, false);
		return newGallery.getResponse();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public GalleryResponse updateGallery(GalleryRequest galleryRequest) throws VempainAclException {
		log.debug("Received gallery request: {}", galleryRequest);
		var currentGallery = galleryRepository.findById(galleryRequest.getId())
		                                      .orElse(null);

		if (currentGallery == null) {
			log.warn("Could not find gallery with ID: {}", galleryRequest.getId());
			return null;
		}

		currentGallery.setShortname(galleryRequest.getShortName());
		currentGallery.setDescription(galleryRequest.getDescription());
		currentGallery.setModifier(accessService.getUserId());
		currentGallery.setModified(Instant.now());
		currentGallery.setLocked(false);

		var updatedGallery = galleryRepository.save(currentGallery);
		galleryFileService.updateGalleryFiles(updatedGallery.getId(), galleryRequest.getSiteFilesId());

		try {
			aclService.updateFromRequestList(galleryRequest.getAcls());
		} catch (Exception e) {
			log.error("Could not update ACLs for gallery with ID: {}", galleryRequest.getId(), e);
			throw new VempainAclException("Could not update ACLs for gallery with ID: " + galleryRequest.getId());
		}

		populateGalleryWithSiteFiles(updatedGallery, false);
		return updatedGallery.getResponse();
	}

	@Transactional
	public void deleteGallery(long galleryId) {
		galleryRepository.deleteById(galleryId);
	}

	private void populateGalleryWithSiteFiles(Gallery gallery, boolean withMetadata) {
		var aclList = aclService.findAclByAclId(gallery.getAclId());
		gallery.setAcls(aclList);
		var galleryFiles = galleryFileService.findGalleryFileByGalleryId(gallery.getId());

		var fileCommons = new ArrayList<SiteFile>();

		for (var galleryFile : galleryFiles) {
			if (withMetadata) {
				siteFileRepository.findById(galleryFile.getSiteFileId())
				                  .ifPresent(fileCommons::add);
			} else {
				siteFileRepository.findByIdWithoutMetadata(galleryFile.getSiteFileId())
				                  .ifPresent(fileCommons::add);
			}
		}

		gallery.setSiteFiles(fileCommons);
	}

	public Iterable<Gallery> findAll() {
		return galleryRepository.findAll();
	}

	@Transactional(readOnly = true)
	public PagedResponse<GalleryResponse> findPagedByUser(PagedRequest request) {
		return findPagedByUser(request, true);
	}

	@Transactional(readOnly = true)
	public PagedResponse<GalleryResponse> findPagedByUserWithoutFiles(PagedRequest request) {
		return findPagedByUser(request, false);
	}

	@Transactional(readOnly = true)
	public PagedResponse<FileGroupListResponse> findPagedGalleryListByUser(PagedRequest request) {
		int safePage = request.getPage();
		int safeSize = Math.min(request.getSize(), 200);
		Sort sortSpec = buildSort(request.getSortBy(), request.getDirection());
		Pageable pageable = PageRequest.of(safePage, safeSize, sortSpec);
		var pageResult = galleryRepository.searchGalleriesForList(request.getSearch(), Boolean.TRUE.equals(request.getCaseSensitive()), pageable);
		var items = new ArrayList<FileGroupListResponse>();

		for (var gallery : pageResult.getContent()) {
			if (accessService.hasReadPermission(gallery.getAclId())) {
				populateGalleryWithAcls(gallery);
				var fileCount = galleryFileService.findGalleryFileByGalleryId(gallery.getId())
				                                  .size();
				items.add(gallery.getListResponse(fileCount));
			}
		}

		return PagedResponse.of(items, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements(),
								pageResult.getTotalPages(), pageResult.isFirst(), pageResult.isLast());
	}

	@Transactional(readOnly = true)
	public FileGroupListResponse findGalleryListById(long galleryId) {
		var gallery = galleryRepository.findById(galleryId)
		                               .orElse(null);
		if (gallery == null || !accessService.hasReadPermission(gallery.getAclId())) {
			return null;
		}
		populateGalleryWithAcls(gallery);
		return gallery.getListResponse(galleryFileService.findGalleryFileByGalleryId(galleryId)
		                                                 .size());
	}

	private PagedResponse<GalleryResponse> findPagedByUser(PagedRequest request, boolean includeFiles) {
		int safePage = request.getPage();
		int safeSize = Math.min(request.getSize(), 200);
		Sort sortSpec = buildSort(request.getSortBy(), request.getDirection());
		Pageable pageable = PageRequest.of(safePage, safeSize, sortSpec);

		var pageResult = includeFiles
						 ? galleryRepository.searchGalleries(request.getSearch(), Boolean.TRUE.equals(request.getCaseSensitive()), pageable)
						 : galleryRepository.searchGalleriesWithoutFiles(request.getSearch(), Boolean.TRUE.equals(request.getCaseSensitive()), pageable);

		var items = new ArrayList<GalleryResponse>();

		for (var gallery : pageResult.getContent()) {
			if (accessService.hasReadPermission(gallery.getAclId())) {
				if (includeFiles) {
					populateGalleryWithSiteFiles(gallery, false);
				} else {
					populateGalleryWithAcls(gallery);
				}
				items.add(gallery.getResponse());
			}
		}

		return PagedResponse.of(items, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements(),
		                        pageResult.getTotalPages(), pageResult.isFirst(), pageResult.isLast());
	}

	private void populateGalleryWithAcls(Gallery gallery) {
		gallery.setAcls(aclService.findAclByAclId(gallery.getAclId()));
	}

	private Sort buildSort(String sort, Sort.Direction direction) {
		Sort.Direction dir = direction == null ? Sort.Direction.ASC : direction;
		return switch (sort == null ? "" : sort.toLowerCase(Locale.ROOT)) {
			case "short_name", "shortname" -> Sort.by(dir, "shortname");
			case "description" -> Sort.by(dir, "description");
			default -> Sort.by(dir, "id");
		};
	}
}
