package fi.poltsi.vempain.admin.service;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import fi.poltsi.vempain.admin.entity.FormComponent;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.service.file.GalleryFileService;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.service.UserService;
import fi.poltsi.vempain.site.entity.WebSiteFile;
import fi.poltsi.vempain.site.entity.WebSitePage;
import fi.poltsi.vempain.site.repository.SiteGalleryRepository;
import fi.poltsi.vempain.site.repository.SitePageRepository;
import fi.poltsi.vempain.site.repository.WebSiteFileRepository;
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

import java.io.File;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PublishService {
	private final SiteFileRepository    siteFileRepository;
	private final SitePageRepository    sitePageRepository;
	private final SiteGalleryRepository siteGalleryRepository;
	private final WebSiteFileRepository webSiteFileRepository;
	private final PageService           pageService;
	private final FormService           formService;
	private final ComponentService      componentService;
	private final FileService           fileService;
	private final LayoutService         layoutService;
	private final UserService           userService;
	private final SubjectService        subjectService;
	private final GalleryFileService    galleryFileService;
	private final SiteSubjectService    siteSubjectService;
	private final PageGalleryService    pageGalleryService;
	private final JschClient            jschClient;

	@Value("${vempain.site.ssh.address}")
	private String siteSshAddress;
	@Value("${vempain.site.ssh.port}")
	private int    siteSshPort;
	@Value("${vempain.site.ssh.user}")
	private String siteSshUser;
	@Value("${vempain.admin.ssh.home-dir}")
	private String adminSshHomeDir;
	@Value("${vempain.admin.ssh.private-key}")
	private String adminSshPrivateKey;

	/// ///////// Pages

	@Transactional
	public void publishAllPages() throws VempainEntityNotFoundException {
		var pages = pageService.findAllByUser();

		for (var page : pages) {
			publishPage(page.getId());
		}

		// Reset the site cache
		sitePageRepository.resetCache();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public long publishPage(Long pageId) throws VempainEntityNotFoundException {
		var page = pageService.findById(pageId);
		var form = formService.findById(page.getFormId());
		var formComponents = formService.findAllFormComponentsByFormId(page.getFormId());
		var layout = layoutService.findById(form.getLayoutId());

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

		var optionalSitePage = sitePageRepository.findByPageId(pageId);
		var creator = userService.findUserResponseById(page.getCreator())
								 .getNick();
		var modifier = "";

		if (page.getModifier() != null) {
			modifier = userService.findUserResponseById(page.getModifier())
								  .getNick();
		} else {
			modifier = null;
		}

		var published = Instant.now();
		WebSitePage webSitePage = optionalSitePage.orElseGet(WebSitePage::new);

		webSitePage.setPageId(page.getId());
		webSitePage.setParentId(page.getParentId());
		webSitePage.setPath(page.getPath());
		webSitePage.setSecure(page.isSecure());
		webSitePage.setIndexList(page.isIndexList());
		webSitePage.setTitle(page.getTitle());
		webSitePage.setHeader(page.getHeader());
		webSitePage.setBody(pageBody);
		webSitePage.setCreator(creator);
		webSitePage.setCreated(page.getCreated());
		webSitePage.setModifier(modifier);
		webSitePage.setModified(page.getModified());
		webSitePage.setCache(null);
		webSitePage.setPublished(published);
		var savedPage = sitePageRepository.save(webSitePage);

		log.debug("Published page: {}", savedPage);
		// We update the page setting the published timestamp
		page.setPublished(published);
		pageService.save(page);

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
		return savedPage.getId();
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
		var fileThumbList = fileService.findAllFileThumbsBySiteFileList(gallery.getSiteFiles());

		for (var fileThumb : fileThumbList) {
			fileThumb.setSiteFile(gallery.getSiteFiles()
										 .stream()
										 .filter(fileCommon -> fileCommon.getId() == fileThumb.getParentId())
										 .findFirst()
										 .orElseThrow(VempainEntityNotFoundException::new));
		}

		// Transfer the files to the site-server
		try {
			log.info("Connecting to site-server {}", siteSshAddress);
			log.debug("Connecting to site-server with user {}", siteSshUser);
			log.debug("Using SSH home dir {}", adminSshHomeDir);
			log.debug("Using SSH private key {}", adminSshPrivateKey);
			jschClient.connect(siteSshAddress, siteSshPort, siteSshUser, adminSshHomeDir, adminSshPrivateKey);
			log.debug("Transferring thumb files to site-server: {}", fileThumbList);
			jschClient.transferFilesToSite(gallery.getSiteFiles(), fileThumbList);
		} catch (JSchException e) {
			log.error("Failed to create a SSH connection to site-server {}", siteSshAddress, e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create a SSH connection to site-server: " + siteSshAddress);
		} catch (SftpException e) {
			log.error("Failed to transfer files to site-server: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to transfer files to site");
		} finally {
			jschClient.close();
		}

		//** Update the site database
		// Remove any existing gallery data if present, the gallery - file relation is removed by cascade
		siteGalleryRepository.deleteByGalleryId(galleryId);

		// Add the gallery
		var siteGallery = gallery.getSiteGallery();
		var newSiteGallery = siteGalleryRepository.save(siteGallery);
		var siteGalleryId = newSiteGallery.getId();

		// File data
		for (var galleryFile : galleryFileList) {
			var siteFile = siteFileRepository.findById(galleryFile.getSiteFileId())
											 .orElseThrow(VempainEntityNotFoundException::new);
			// Remove the web site file if it exists
			log.debug("Deleting web site file by file ID: {}", siteFile.getId());
			webSiteFileRepository.deleteByFileId(siteFile.getId());

			var webSiteFile = WebSiteFile.builder()
										 .fileId(siteFile.getId())
										 .comment(siteFile.getComment())
										 .path(siteFile.getFileType().shortName + File.separator + siteFile.getFilePath() + File.separator + siteFile.getFileName())
										 .mimetype(siteFile.getMimeType())
										 .metadata(siteFile.getMetadata())
										 .build();

			log.debug("Saving site file: {}", webSiteFile);
			var newSiteFile = webSiteFileRepository.save(webSiteFile);
			// Add new gallery file relation
			siteGalleryRepository.saveGalleryFile(siteGalleryId, newSiteFile.getId(), galleryFile.getSortOrder());
			// Save subject on site-side
			var subjects = subjectService.getSubjectsByFileId(webSiteFile.getId());
			var siteSubjects = siteSubjectService.saveAllFromAdminSubject(subjects);
			// Save subject - file relation on site-side
			siteSubjectService.saveSiteFileSubject(newSiteFile.getId(), siteSubjects);
		}
	}

	public Optional<WebSitePage> fetchSitePage(Long pageId) {
		return sitePageRepository.findById(pageId);
	}

	@Transactional
	public void publishAllGalleries() throws VempainEntityNotFoundException {
		var galleries = fileService.findAllGalleries();

		for (var gallery : galleries) {
			publishGallery(gallery.getId());
		}
	}
}
