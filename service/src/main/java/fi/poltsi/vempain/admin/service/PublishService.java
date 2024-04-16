package fi.poltsi.vempain.admin.service;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import fi.poltsi.vempain.admin.api.FileClassEnum;
import fi.poltsi.vempain.admin.entity.AbstractVempainEntity;
import fi.poltsi.vempain.admin.entity.FormComponent;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.admin.repository.file.FileImagePageableRepository;
import fi.poltsi.vempain.admin.repository.file.FileVideoPageableRepository;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.service.file.GalleryFileService;
import fi.poltsi.vempain.site.entity.SiteFile;
import fi.poltsi.vempain.site.entity.SitePage;
import fi.poltsi.vempain.site.repository.SiteFileRepository;
import fi.poltsi.vempain.site.repository.SiteGalleryRepository;
import fi.poltsi.vempain.site.repository.SitePageRepository;
import fi.poltsi.vempain.site.service.SiteSubjectService;
import fi.poltsi.vempain.tools.JschClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PublishService {
	private final PageService                 pageService;
	private final FormService                 formService;
	private final ComponentService            componentService;
	private final FileService                 fileService;
	private final FileImagePageableRepository fileImagePageableRepository;
	private final FileVideoPageableRepository fileVideoPageableRepository;
	private final LayoutService               layoutService;
	private final UserService                 userService;
	private final SubjectService              subjectService;
	private final SitePageRepository          sitePageRepository;
	private final SiteFileRepository          siteFileRepository;
	private final SiteGalleryRepository       siteGalleryRepository;
	private final GalleryFileService          galleryFileService;
	private final SiteSubjectService          siteSubjectService;
	private final PageGalleryService          pageGalleryService;
	private final JschClient                  jschClient;

	@Value("${vempain.site.ssh.address}")
	private String siteSshAddress;
	@Value("${vempain.site.ssh.port}")
	private int    siteSshPort;
	@Value("${vempain.site.ssh.user}")
	private String siteSshUser;
	@Value("${vempain.admin.ssh.config-dir}")
	private String adminSshConfigDir;
	@Value("${vempain.admin.ssh.private-key}")
	private String adminSshPrivateKey;

	//////////// Pages

	public void publishAllPages() throws VempainEntityNotFoundException {
		var pages = pageService.findAllByUser();

		for (var page : pages) {
			publishPage(page.getId());
		}

		// Reset the site cache
		sitePageRepository.resetCache();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void publishPage(Long pageId) throws VempainEntityNotFoundException {
		var page           = pageService.findById(pageId);
		var form           = formService.findById(page.getFormId());
		var formComponents = formService.findAllFormComponentsByFormId(page.getFormId());
		var layout         = layoutService.findById(form.getLayoutId());

		var pageBody = layout.getStructure();
		pageBody = pageBody.replace("<!--page-->", page.getBody());
		// Add PHP-tags to the beginning and end of body as the default content for layout is HTML
		// See this why there is a space at the end: http://php.net/manual/en/function.eval.php#97063
		pageBody = "?>" + pageBody + "<?php ";
		var i = 0;

		for (FormComponent formComponent : formComponents) {
			try {
				var component = componentService.findById(formComponent.getComponentId());
				// The default content for component is PHP, so we enclose the component content with PHP tags
				pageBody = pageBody.replace("<!--comp_" + i + "-->", "<?php\n" + component.getCompData() + "\n?>");
			} catch (VempainComponentException e) {
				log.error("Failed to fetch component ({}) for form {}", formComponent.getComponentId(), form.getId());
			}

			i++;
		}

		var optionalSitePage = sitePageRepository.findById(pageId);
		var creator          = userService.findUserResponseById(page.getCreator()).getNick();
		var modifier         = "";

		if (page.getModifier() != null) {
			modifier = userService.findUserResponseById(page.getModifier()).getNick();
		} else {
			modifier = null;
		}

		SitePage     sitePage      = optionalSitePage.orElseGet(SitePage::new);

		sitePage.setId(page.getId());
		sitePage.setParentId(page.getParentId());
		sitePage.setPath(page.getPath());
		sitePage.setSecure(page.isSecure());
		sitePage.setIndexList(page.isIndexList());
		sitePage.setTitle(page.getTitle());
		sitePage.setHeader(page.getHeader());
		sitePage.setBody(pageBody);
		sitePage.setCreator(creator);
		sitePage.setCreated(page.getCreated());
		sitePage.setModifier(modifier);
		sitePage.setModified(page.getModified());
		sitePage.setCache(null);
		sitePage.setPublished(Instant.now());
		var savedPage = sitePageRepository.save(sitePage);

		// Check if there are any galleries in the page, if then they should also be published
		var pageGalleries = pageGalleryService.findPageGalleryByPageId(pageId);

		if (!pageGalleries.isEmpty()) {
			for (var pageGallery : pageGalleries) {
				publishGallery(pageGallery.getGalleryId());
			}
		}

		// Finally, we want to reset the cache for the page which includes the Top10 component
		// Currently hard coded to page ID 10 which is the front page
		sitePageRepository.resetCacheByPageId(10L);
	}

	public void deletePage(Long pageId) {
		sitePageRepository.deletePageById(pageId);
	}

	//////////// Gallery

	/**
	 * This publishes to the site the auxiliary files belonging to a gallery. This should either be called from the Admin UI
	 * when updating an existing gallery, or in connection to publishing a page when it is detected to contain a gallery
	 *
	 * @param galleryId ID of the gallery to be published
	 */

	@Transactional(propagation = Propagation.REQUIRED)
	public void publishGallery(Long galleryId) throws VempainEntityNotFoundException {
		var gallery = fileService.findGalleryById(galleryId);

		if (gallery == null) {
			log.error("Failed to publish a non-existing gallery by ID: {}", galleryId);
			throw new VempainEntityNotFoundException();
		}

		var galleryFileList = galleryFileService.findGalleryFileByGalleryId(galleryId);

		if (galleryFileList.isEmpty()) {
			log.warn("Gallery {} does not contain any files. There is nothing to publish", galleryId);
			return;
		}

		// Fetch the common and thumb files
		var fileThumbList = fileService.findAllFileThumbsByFileCommonList(gallery.getCommonFiles());

		for (var fileThumb : fileThumbList) {
			fileThumb.setFileCommon(gallery.getCommonFiles()
										   .stream()
										   .filter(fileCommon -> fileCommon.getId() == fileThumb.getParentId())
										   .findFirst()
										   .orElseThrow(VempainEntityNotFoundException::new));
		}

		try {
			log.info("Connecting to site-server {}", siteSshAddress);
			log.debug("Connecting to site-server with user {}", siteSshUser);
			log.debug("Using SSH config dir {}", adminSshConfigDir);
			log.debug("Using SSH private key {}", adminSshPrivateKey);
			jschClient.connect(siteSshAddress, siteSshPort, siteSshUser, adminSshConfigDir, adminSshPrivateKey);
			log.debug("Transferring files to site-server");
			jschClient.transferFilesToSite(gallery.getCommonFiles(), fileThumbList);
		} catch (JSchException e) {
			log.error("Failed to create a SSH connection to site-server {}", siteSshAddress, e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create a SSH connection to site-server: " + siteSshAddress);
		} catch (SftpException e) {
			log.error("Failed to transfer files to site-server", e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to transfer files to site");
		} finally {
			jschClient.close();
		}

		//// Update the site database
		// Remove any existing gallery data if present, the gallery - file relation is removed by cascade
		siteGalleryRepository.deleteById(galleryId);
		// File data
		for (var fileCommon : gallery.getCommonFiles()) {
			// Remove the site file if it exists
			siteFileRepository.deleteById(fileCommon.getId());

			var siteFile = SiteFile.builder()
								   .id(fileCommon.getId())
								   .comment(fileCommon.getComment())
								   .path(fileCommon.getSiteFilename())
								   .mimetype(fileCommon.getMimetype())
								   .metadata(fileCommon.getMetadata())
								   .build();

			// Depending on the mimetype, we can fetch the width and height value if the file class is either image or video
			if ((FileClassEnum.getFileClassByOrder(fileCommon.getFileClassId()) == FileClassEnum.IMAGE)
				|| (FileClassEnum.getFileClassByOrder(fileCommon.getFileClassId()) == FileClassEnum.VIDEO)) {
				var imageFile = fileImagePageableRepository.findById(fileCommon.getId());

				// Images are shrunk to a smaller size determined by vempain.site.image-size at transfer time, and the dimensions are stored
				// as transient field in the object
				if (imageFile.isPresent()) {
					siteFile.setWidth(fileCommon.getSiteFileDimension().width);
					siteFile.setHeight(fileCommon.getSiteFileDimension().height);
				} else {
					var videoFile = fileVideoPageableRepository.findById(fileCommon.getId());

					if (videoFile.isPresent()) {
						siteFile.setWidth(videoFile.get().getWidth());
						siteFile.setHeight(videoFile.get().getHeight());
						siteFile.setLength(videoFile.get().getLength());
					}
				}
			}

			siteFileRepository.save(siteFile);
		}
		// Add the gallery
		siteGalleryRepository.saveGallery(gallery.getSiteGallery());
		// Add the gallery files
		for (var galleryFile : galleryFileList) {
			siteGalleryRepository.saveGalleryFile(galleryId, galleryFile.getFileCommonId(), galleryFile.getSortOrder());
		}
		// Subject data
		var fileCommonIds = gallery.getCommonFiles()
								   .stream()
								   .mapToLong(AbstractVempainEntity::getId)
								   .toArray();
		var fileSubjects = subjectService.getCommonFileSubjectListMap(fileCommonIds);

		for (var fileCommonId : fileSubjects.keySet()) {
			var subjects = fileSubjects.get(fileCommonId);
			siteSubjectService.saveAllFromAdminSubject(subjects);
			siteSubjectService.saveSiteFileSubject(fileCommonId, subjects);
		}
	}

	public Optional<SitePage> fetchSitePage(Long pageId) {
		return sitePageRepository.findById(pageId);
	}

	public void publishAllGalleries() throws VempainEntityNotFoundException {
		var galleries = fileService.findAllGalleries();

		for (var gallery : galleries) {
			publishGallery(gallery.getId());
		}
	}
}
