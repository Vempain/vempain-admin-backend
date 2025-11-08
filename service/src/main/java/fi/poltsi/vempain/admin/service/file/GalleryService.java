package fi.poltsi.vempain.admin.service.file;

import fi.poltsi.vempain.admin.api.QueryDetailEnum;
import fi.poltsi.vempain.admin.api.request.file.GalleryRequest;
import fi.poltsi.vempain.admin.api.response.file.GalleryResponse;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.entity.file.SiteFile;
import fi.poltsi.vempain.admin.repository.file.GalleryRepository;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.AccessService;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.service.AclService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
				populateGalleryWithSiteFiles(gallery);
				galleryList.add(gallery);
			}
		}

		return galleryList;
	}

	public List<GalleryResponse> findAllAsResponsesForUser(QueryDetailEnum queryDetailEnum) {
		var galleries = findAllForUser();
		var responses = new ArrayList<GalleryResponse>();

		for (Gallery gallery : galleries) {
			if (queryDetailEnum == QueryDetailEnum.FULL) {
				populateGalleryWithSiteFiles(gallery);
			}

			var response = gallery.getResponse();
			responses.add(response);
		}

		return responses;
	}

	public GalleryResponse findById(long galleryId) {
		var gallery = galleryRepository.findById(galleryId)
									   .orElse(null);

		if (gallery == null) {
			return null;
		}

		if (!accessService.hasReadPermission(gallery.getAclId())) {
			return null;
		}

		populateGalleryWithSiteFiles(gallery);
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
		galleryFileService.addGalleryFiles(newGallery.getId(), galleryRequest.getCommonFilesId());

		try {
			aclService.saveAclRequests(aclId, galleryRequest.getAcls());
		} catch (Exception e) {
			log.error("Could not create ACLs for new gallery with ID: {}", newGallery.getId(), e);
			throw new VempainAclException("Could not create ACLs for new gallery with ID: " + newGallery.getId());
		}

		populateGalleryWithSiteFiles(newGallery);
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
		galleryFileService.updateGalleryFiles(updatedGallery.getId(), galleryRequest.getCommonFilesId());

		try {
			aclService.updateFromRequestList(galleryRequest.getAcls());
		} catch (Exception e) {
			log.error("Could not update ACLs for gallery with ID: {}", galleryRequest.getId(), e);
			throw new VempainAclException("Could not update ACLs for gallery with ID: " + galleryRequest.getId());
		}

		populateGalleryWithSiteFiles(updatedGallery);
		return updatedGallery.getResponse();
	}

	@Transactional
	public void deleteGallery(long galleryId) {
		galleryRepository.deleteById(galleryId);
	}

	private void populateGalleryWithSiteFiles(Gallery gallery) {
		var aclList = aclService.findAclByAclId(gallery.getAclId());
		gallery.setAcls(aclList);
		var galleryFiles = galleryFileService.findGalleryFileByGalleryId(gallery.getId());

		var fileCommons = new ArrayList<SiteFile>();

		for (var galleryFile : galleryFiles) {
			siteFileRepository.findById(galleryFile.getSiteFileId())
							  .ifPresent(fileCommons::add);
		}

		gallery.setSiteFiles(fileCommons);
	}

	public Iterable<Gallery> findAll() {
		return galleryRepository.findAll();
	}
}
