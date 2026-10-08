package fi.poltsi.vempain.admin.service;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import fi.poltsi.vempain.admin.api.PublishResultEnum;
import fi.poltsi.vempain.admin.api.TaskTypeEnum;
import fi.poltsi.vempain.admin.api.response.PublishResponse;
import fi.poltsi.vempain.admin.entity.FormComponent;
import fi.poltsi.vempain.admin.entity.Page;
import fi.poltsi.vempain.admin.entity.file.Gallery;
import fi.poltsi.vempain.admin.exception.VempainComponentException;
import fi.poltsi.vempain.admin.repository.file.SiteFileRepository;
import fi.poltsi.vempain.admin.service.file.FileService;
import fi.poltsi.vempain.admin.service.file.GalleryFileService;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.service.UserService;
import fi.poltsi.vempain.common.api.FileTypeEnum;
import fi.poltsi.vempain.common.api.response.TaskAcceptedResponse;
import fi.poltsi.vempain.common.task.TaskProgress;
import fi.poltsi.vempain.common.task.TaskRunner;
import fi.poltsi.vempain.site.entity.WebGpsLocation;
import fi.poltsi.vempain.site.entity.WebSitePage;
import fi.poltsi.vempain.site.repository.WebGpsLocationRepository;
import fi.poltsi.vempain.site.repository.WebSiteFileRepository;
import fi.poltsi.vempain.site.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.site.repository.WebSitePageRepository;
import fi.poltsi.vempain.site.service.WebSiteResourceService;
import fi.poltsi.vempain.site.service.WebSiteSubjectService;
import fi.poltsi.vempain.tools.JschClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fi.poltsi.vempain.auth.tools.JsonTools.toJson;

@Slf4j
@RequiredArgsConstructor
@Service
public class PublishService {
	private final SiteFileRepository siteFileRepository;
	private final WebSitePageRepository    webSitePageRepository;
	private final WebSiteGalleryRepository webSiteGalleryRepository;
	private final WebSiteFileRepository    webSiteFileRepository;
	private final WebGpsLocationRepository webGpsLocationRepository;

	private final PageService           pageService;
	private final FormService           formService;
	private final ComponentService      componentService;
	private final FileService           fileService;
	private final LayoutService         layoutService;
	private final UserService           userService;
	private final SubjectService        subjectService;
	private final GalleryFileService    galleryFileService;
	private final WebSiteSubjectService webSiteSubjectService;
	private final PageGalleryService    pageGalleryService;
	private final JschClient            jschClient;
	private final WebSiteResourceService webSiteResourceService;
	private final AccessService accessService;
	private final TaskRunner         taskRunner;
	private final ApplicationContext applicationContext;

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

	@Value("${vempain.site.thumb-directory}")
	private String thumbSubDir;

	/// ///////// Pages

	@Transactional
	public void publishAllPages() throws VempainEntityNotFoundException {
		var pages = pageService.findAllByUser();

		for (var page : pages) {
			if (!canPublishPage(page)) {
				log.warn("Skipping publish for page {} because the user lacks permission on the page or one of its linked entities", page.getId());
				continue;
			}

			publishPageAsSystem(page.getId());
		}

		// Reset the site cache
		webSitePageRepository.resetCache();
	}

	/**
	 * Publishes a page on behalf of the current user. The user needs the modify privilege on the page and the read privilege on the
	 * form, layout and components it is built from; every gallery attached to the page is published too, so the user must also be
	 * allowed to publish those galleries and read their files.
	 *
	 * @throws AccessDeniedException when the current user may not publish the page or one of its linked entities
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public long publishPage(Long pageId) throws VempainEntityNotFoundException {
		authorizePagePublish(pageId);
		return publishPageAsSystem(pageId);
	}

	/**
	 * Verifies that the current user may publish the page and everything it depends on.
	 *
	 * @throws VempainEntityNotFoundException when the page does not exist
	 * @throws AccessDeniedException          when the current user lacks a required privilege
	 */
	public void authorizePagePublish(long pageId) throws VempainEntityNotFoundException {
		var page = pageService.findById(pageId);

		if (page == null) {
			throw new VempainEntityNotFoundException("Page not found by id: " + pageId, "page");
		}

		if (!canPublishPage(page)) {
			throw new AccessDeniedException("User does not have permission to publish page " + pageId + " or one of its linked entities");
		}
	}

	/**
	 * @return {@code true} when the current user holds the modify privilege on the page, the read privilege on its form, layout and
	 * components, and may publish every gallery attached to the page (including reading all of the gallery files)
	 */
	public boolean canPublishPage(Page page) {
		if (page == null || !accessService.hasModifyPermission(page.getAclId())) {
			return false;
		}

		try {
			var form = formService.findById(page.getFormId());

			if (!accessService.hasReadPermission(form.getAclId())) {
				return false;
			}

			var layout = layoutService.findById(form.getLayoutId());

			if (!accessService.hasReadPermission(layout.getAclId())) {
				return false;
			}

			for (FormComponent formComponent : formService.findAllFormComponentsByFormId(page.getFormId())) {
				try {
					var component = componentService.findById(formComponent.getComponentId());

					if (!accessService.hasReadPermission(component.getAclId())) {
						return false;
					}
				} catch (VempainComponentException e) {
					// A missing component is skipped by the publish as well, it cannot leak anything
					log.warn("Component {} referenced by form {} does not exist", formComponent.getComponentId(), form.getId());
				}
			}
		} catch (VempainEntityNotFoundException e) {
			log.warn("Page {} refers to a missing form or layout: {}", page.getId(), e.getMessage());
			return false;
		}

		for (var pageGallery : pageGalleryService.findPageGalleryByPageId(page.getId())) {
			if (!canPublishGallery(pageGallery.getGalleryId())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Publishes a page without checking the caller's privileges. Only for system initiated work such as {@code PublishItemSchedule},
	 * which executes publishes that were authorized when they were scheduled.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public long publishPageAsSystem(Long pageId) throws VempainEntityNotFoundException {
		var page = pageService.findById(pageId);

		if (page == null) {
			throw new VempainEntityNotFoundException("Page not found by id: " + pageId, "page");
		}

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

		// Remove any extra empty PHP tags
		// Ending PGP-tag followed by white space and starting PHP-tag which then again is followed by white space and ending PHP-tag
		pageBody = pageBody.replaceAll("\\?>\\s*<\\?php\\s*\\?>", "?>");
		// Starting PHP-tag followed by white space and ending PHP-tag
		pageBody = pageBody.replaceAll("<\\?php\\s*\\?>", "");

		var optionalSitePage = webSitePageRepository.findByPageId(pageId);
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
		var webSitePage = optionalSitePage.orElseGet(WebSitePage::new);

		if (webSitePage.getAclId() == 0) {
			webSitePage.setAclId(webSiteResourceService.getNextWebSiteAcl());
		}

		webSitePage.setPageId(page.getId());
		webSitePage.setParentId(page.getParentId());
		webSitePage.setFilePath(page.getPagePath());
		webSitePage.setSecure(page.isSecure());
		webSitePage.setIndexList(page.isIndexList());
		webSitePage.setTitle(page.getTitle());
		webSitePage.setHeader(page.getHeader());
		webSitePage.setBody(pageBody);
		webSitePage.setPageStyle(page.getPageStyle());
		webSitePage.setCreator(creator);
		webSitePage.setCreated(page.getCreated());
		webSitePage.setModifier(modifier);
		webSitePage.setModified(page.getModified());
		webSitePage.setCache(null);
		webSitePage.setPublished(published);
		var savedPage = webSitePageRepository.save(webSitePage);

		log.debug("Published page: {}", savedPage);
		// We update the page setting the published timestamp
		page.setPublished(published);
		pageService.save(page);

		// Check if there are any galleries in the page, if then they should also be published
		var pageGalleries = pageGalleryService.findPageGalleryByPageId(pageId);

		if (!pageGalleries.isEmpty()) {
			for (var pageGallery : pageGalleries) {
				publishGalleryAsSystem(pageGallery.getGalleryId());
			}
		}

		// Finally, we want to reset the cache for the page which includes the Top10 component
		// Currently hard coded to page ID 10 which is the front page
		webSitePageRepository.resetCacheByPageId(10L);
		return savedPage.getId();
	}

	public void deletePage(Long pageId) {
		webSitePageRepository.deletePageById(pageId);
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
		authorizeGalleryPublish(galleryId);
		publishGalleryAsSystem(galleryId);
	}

	/**
	 * Verifies that the current user may publish the gallery: modify privilege on the gallery and read privilege on every file in it.
	 *
	 * @throws VempainEntityNotFoundException when the gallery does not exist
	 * @throws AccessDeniedException          when the current user lacks a required privilege
	 */
	public void authorizeGalleryPublish(long galleryId) throws VempainEntityNotFoundException {
		var gallery = fileService.findGalleryById(galleryId);

		if (gallery == null) {
			log.error("Failed to publish a non-existing gallery by ID: {}", galleryId);
			throw new VempainEntityNotFoundException();
		}

		if (!canPublishGallery(gallery)) {
			throw new AccessDeniedException("User does not have permission to publish gallery " + galleryId + " or to read all of its files");
		}
	}

	public boolean canPublishGallery(long galleryId) {
		return canPublishGallery(fileService.findGalleryById(galleryId));
	}

	/**
	 * @return {@code true} when the current user holds the modify privilege on the gallery and the read privilege on each of its site files
	 */
	public boolean canPublishGallery(Gallery gallery) {
		if (gallery == null || !accessService.hasModifyPermission(gallery.getAclId())) {
			return false;
		}

		var siteFiles = gallery.getSiteFiles();

		if (siteFiles == null || siteFiles.isEmpty()) {
			return true;
		}

		return siteFiles.stream()
						.allMatch(siteFile -> accessService.hasReadPermission(siteFile.getAclId()));
	}

	/**
	 * Publishes a gallery without checking the caller's privileges. Only for system initiated work ({@code PublishItemSchedule} and page
	 * publishing, which has already authorized the attached galleries).
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void publishGalleryAsSystem(Long galleryId) throws VempainEntityNotFoundException {
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
			log.debug("Connecting to site-server {}", siteSshAddress);
			log.debug("Connecting to site-server with user {}", siteSshUser);
			log.debug("Using SSH home dir {}", adminSshHomeDir);
			log.debug("Using SSH private key {}", adminSshPrivateKey);
			jschClient.connect(siteSshAddress, siteSshPort, siteSshUser, adminSshHomeDir, adminSshPrivateKey);
			log.debug("Transferring thumb files to site-server: {}", toJson(fileThumbList));
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
		webSiteGalleryRepository.deleteByGalleryId(galleryId);

		// Add the gallery
		var siteGallery = gallery.getSiteGallery();
		siteGallery.setAclId(webSiteResourceService.getNextWebSiteAcl());
		var newSiteGallery = webSiteGalleryRepository.save(siteGallery);
		var siteGalleryId = newSiteGallery.getId();

		// File data
		for (var galleryFile : galleryFileList) {
			var siteFile = siteFileRepository.findById(galleryFile.getSiteFileId())
			                                 .orElseThrow(VempainEntityNotFoundException::new);
			// Remove the web site file if it exists
			log.debug("Deleting potential web site file by file ID: {}", siteFile.getId());
			webSiteFileRepository.deleteByFileId(siteFile.getId());

			// Map / upsert GPS location into site DB (separate persistence unit)
			WebGpsLocation webLocation = null;

			if (siteFile.getLocation() != null) {
				var adminLoc = siteFile.getLocation();
				webLocation = webGpsLocationRepository.findById(adminLoc.getId())
				                                      .orElseGet(() -> WebGpsLocation.builder()
				                                                                     .id(adminLoc.getId())
				                                                                     .build());
				webLocation.setLatitude(adminLoc.getLatitude());
				webLocation.setLatitudeRef(adminLoc.getLatitudeRef());
				webLocation.setLongitude(adminLoc.getLongitude());
				webLocation.setLongitudeRef(adminLoc.getLongitudeRef());
				webLocation.setAltitude(adminLoc.getAltitude());
				webLocation.setDirection(adminLoc.getDirection());
				webLocation.setSatelliteCount(adminLoc.getSatelliteCount());
				webLocation.setCountry(adminLoc.getCountry());
				webLocation.setState(adminLoc.getState());
				webLocation.setCity(adminLoc.getCity());
				webLocation.setStreet(adminLoc.getStreet());
				webLocation.setSubLocation(adminLoc.getSubLocation());
				webLocation = webGpsLocationRepository.save(webLocation);
			}

			// When creating
			var webSiteFile = siteFile.toWebSiteFile();
			webSiteFile.setAclId(webSiteResourceService.getNextWebSiteAcl());
			webSiteFile.setFilePath(siteFile.getFileType().shortName + File.separator + siteFile.getFilePath() + File.separator + siteFile.getFileName());
			webSiteFile.setLocation(webLocation);

			// Set thumbnail path for image files
			if (siteFile.getFileType() == FileTypeEnum.IMAGE) {
				webSiteFile.setThumbnailPath(siteFile.getFileType().shortName + File.separator + siteFile.getFilePath() + File.separator + thumbSubDir + File.separator + siteFile.getFileName());
			}

			log.debug("Saving web site file: {} with metadata length {} from siteFile metadata length {}", toJson(webSiteFile),
			          (webSiteFile.getMetadata() != null ? webSiteFile.getMetadata()
			                                                          .length() : 0),
			          (webSiteFile.getMetadata() != null ? siteFile.getMetadata()
			                                                       .length() : 0));
			var newWebSiteFile = webSiteFileRepository.save(webSiteFile);
			// Add new gallery file relation
			webSiteGalleryRepository.saveGalleryFile(siteGalleryId, newWebSiteFile.getId(), galleryFile.getSortOrder());
			// Save subject on site-side
			var subjects = subjectService.getSubjectsByFileId(siteFile.getId());
			log.debug("Publishing subjects for site file {}: {}", newWebSiteFile.getId(), toJson(subjects));
			var siteSubjects = webSiteSubjectService.saveAllFromAdminSubject(subjects);
			// Save subject - file relation on site-side
			webSiteSubjectService.saveSiteFileSubject(newWebSiteFile.getId(), siteSubjects);
		}
	}

	public Optional<WebSitePage> fetchSitePage(Long pageId) {
		return webSitePageRepository.findById(pageId);
	}

	@Transactional
	public void publishAllGalleries() throws VempainEntityNotFoundException {
		var galleries = fileService.findAllGalleries();

		for (var gallery : galleries) {
			if (!canPublishGallery(gallery.getId())) {
				log.warn("Skipping publish for gallery {} because the user lacks permission on the gallery or its files", gallery.getId());
				continue;
			}

			publishGalleryAsSystem(gallery.getId());
		}
	}

	@Transactional
	public PublishResponse publishSelectedGalleries(List<Long> galleryIds) {
		if (galleryIds == null || galleryIds.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gallery ID list cannot be empty");
		}

		var publishedCount = 0L;

		for (var galleryId : galleryIds) {
			if (galleryId == null || galleryId < 1) {
				log.warn("Skipping invalid gallery ID in publishSelectedGalleries: {}", galleryId);
				continue;
			}

			var gallery = fileService.findGalleryById(galleryId);
			if (gallery == null) {
				log.warn("Skipping publish for gallery {} because it does not exist", galleryId);
				continue;
			}

			if (!canPublishGallery(gallery)) {
				log.warn("Skipping publish for gallery {} because user lacks modify permission on it or read permission on its files", galleryId);
				continue;
			}

			try {
				publishGalleryAsSystem(galleryId);
				publishedCount++;
			} catch (VempainEntityNotFoundException e) {
				log.warn("Failed to publish gallery {}: {}", galleryId, e.getMessage());
			}
		}

		var skipped = galleryIds.size() - publishedCount;
		var result = publishedCount > 0 ? PublishResultEnum.OK : PublishResultEnum.FAIL;
		var message = "Published " + publishedCount + " galleries, skipped " + skipped;
		return PublishResponse.builder()
		                      .result(result)
		                      .message(message)
		                      .timestamp(Instant.now())
		                      .build();
	}

	/// ///////// Background tasks (shared task facility)

	/**
	 * Starts a background task that publishes one page and its galleries. The caller's privileges are verified synchronously before
	 * the task is submitted, exactly as {@link #publishPage(Long)} would.
	 *
	 * @throws AccessDeniedException          when the current user may not publish the page or one of its linked entities
	 * @throws VempainEntityNotFoundException when the page does not exist
	 */
	public TaskAcceptedResponse publishPageAsTask(long pageId) throws VempainEntityNotFoundException {
		authorizePagePublish(pageId);
		var page = pageService.findById(pageId);
		var progress = taskRunner.submitDurable(TaskTypeEnum.PUBLISH_PAGE.name(), "Publish page " + page.getTitle(), 1, Map.of("page_id", pageId),
												task -> publishPageNow(pageId, task));
		return progress.toAcceptedResponse();
	}

	/**
	 * Task body of {@link TaskTypeEnum#PUBLISH_PAGE}; the publish itself runs through the Spring proxy so that it is transactional.
	 */
	public Map<String, Object> publishPageNow(long pageId, TaskProgress progress) throws VempainEntityNotFoundException {
		progress.checkpoint();
		var sitePageId = publishServiceProxy().publishPageAsSystem(pageId);
		progress.advance("Published page " + pageId);
		return Map.of("site_page_id", sitePageId);
	}

	/**
	 * Starts a background task that publishes every page the current user may publish. Pages the user may not publish are skipped
	 * when the task is submitted, so the task only contains authorized work.
	 */
	public TaskAcceptedResponse publishAllPagesAsTask() {
		var pageIds = new ArrayList<Long>();

		for (var page : pageService.findAllByUser()) {
			if (canPublishPage(page)) {
				pageIds.add(page.getId());
			} else {
				log.warn("Skipping publish for page {} because the user lacks permission on the page or one of its linked entities", page.getId());
			}
		}

		var ids = List.copyOf(pageIds);
		var progress = taskRunner.submitDurable(TaskTypeEnum.PUBLISH_ALL_PAGES.name(), "Publish all pages", ids.size(), Map.of("page_ids", ids),
												task -> publishPagesNow(ids, task));
		return progress.toAcceptedResponse();
	}

	/**
	 * Task body of {@link TaskTypeEnum#PUBLISH_ALL_PAGES}: one step per page, a missing page fails its step without failing the task.
	 */
	public Map<String, Object> publishPagesNow(List<Long> pageIds, TaskProgress progress) {
		var published = 0L;

		for (var pageId : pageIds) {
			progress.checkpoint();

			try {
				publishServiceProxy().publishPageAsSystem(pageId);
				published++;
				progress.advance("Published page " + pageId);
			} catch (VempainEntityNotFoundException e) {
				log.warn("Failed to publish page {}: {}", pageId, e.getMessage());
				progress.advanceFailed("Page " + pageId + " could not be published: " + e.getMessage());
			}
		}

		// Reset the site cache
		webSitePageRepository.resetCache();
		return publishCounts(published, pageIds.size() - published);
	}

	/**
	 * Starts a background task that publishes one gallery; the privileges are verified synchronously like in {@link #publishGallery(Long)}.
	 */
	public TaskAcceptedResponse publishGalleryAsTask(long galleryId) throws VempainEntityNotFoundException {
		authorizeGalleryPublish(galleryId);
		var gallery = fileService.findGalleryById(galleryId);
		var ids = List.of(galleryId);
		var progress = taskRunner.submitDurable(TaskTypeEnum.PUBLISH_GALLERY.name(), "Publish gallery " + gallery.getShortname(), 1,
												Map.of("gallery_ids", ids, "skipped", 0), task -> publishGalleriesNow(ids, 0, task));
		return progress.toAcceptedResponse();
	}

	/**
	 * Starts a background task that publishes every gallery the current user may publish; the others are skipped at submit time.
	 */
	public TaskAcceptedResponse publishAllGalleriesAsTask() {
		var galleryIds = new ArrayList<Long>();
		var skipped = 0;

		for (var gallery : fileService.findAllGalleries()) {
			if (canPublishGallery(gallery.getId())) {
				galleryIds.add(gallery.getId());
			} else {
				log.warn("Skipping publish for gallery {} because the user lacks permission on the gallery or its files", gallery.getId());
				skipped++;
			}
		}

		return submitGalleryPublish(TaskTypeEnum.PUBLISH_ALL_GALLERIES, "Publish all galleries", List.copyOf(galleryIds), skipped);
	}

	/**
	 * Starts a background task that publishes the selected galleries the current user may publish. Invalid, missing and
	 * unauthorized galleries are skipped at submit time and counted in the task result.
	 */
	public TaskAcceptedResponse publishSelectedGalleriesAsTask(List<Long> requestedIds) {
		if (requestedIds == null || requestedIds.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gallery ID list cannot be empty");
		}

		var galleryIds = new ArrayList<Long>();

		for (var galleryId : requestedIds) {
			if (galleryId == null || galleryId < 1) {
				log.warn("Skipping invalid gallery ID in publishSelectedGalleriesAsTask: {}", galleryId);
				continue;
			}

			var gallery = fileService.findGalleryById(galleryId);

			if (gallery == null) {
				log.warn("Skipping publish for gallery {} because it does not exist", galleryId);
				continue;
			}

			if (!canPublishGallery(gallery)) {
				log.warn("Skipping publish for gallery {} because user lacks modify permission on it or read permission on its files", galleryId);
				continue;
			}

			galleryIds.add(galleryId);
		}

		return submitGalleryPublish(TaskTypeEnum.PUBLISH_SELECTED_GALLERIES, "Publish " + galleryIds.size() + " selected galleries",
									List.copyOf(galleryIds), requestedIds.size() - galleryIds.size());
	}

	private TaskAcceptedResponse submitGalleryPublish(TaskTypeEnum type, String title, List<Long> galleryIds, int skipped) {
		var progress = taskRunner.submitDurable(type.name(), title, galleryIds.size(), Map.of("gallery_ids", galleryIds, "skipped", skipped),
												task -> publishGalleriesNow(galleryIds, skipped, task));
		return progress.toAcceptedResponse();
	}

	/**
	 * Task body of the gallery publish tasks: one step per gallery, a missing gallery fails its step without failing the task.
	 *
	 * @param skippedBeforeStart galleries that were left out when the task was submitted, reported in the result
	 */
	public Map<String, Object> publishGalleriesNow(List<Long> galleryIds, int skippedBeforeStart, TaskProgress progress) {
		var published = 0L;

		for (var galleryId : galleryIds) {
			progress.checkpoint();

			try {
				publishServiceProxy().publishGalleryAsSystem(galleryId);
				published++;
				progress.advance("Published gallery " + galleryId);
			} catch (VempainEntityNotFoundException e) {
				log.warn("Failed to publish gallery {}: {}", galleryId, e.getMessage());
				progress.advanceFailed("Gallery " + galleryId + " could not be published: " + e.getMessage());
			}
		}

		return publishCounts(published, galleryIds.size() - published + skippedBeforeStart);
	}

	private static Map<String, Object> publishCounts(long published, long skipped) {
		return Map.of("published", published, "skipped", skipped);
	}

	/**
	 * The transactional proxy of this service; task bodies run on worker threads outside the request and must call the publish
	 * methods through it so that each item gets its own transaction.
	 */
	private PublishService publishServiceProxy() {
		return applicationContext.getBean(PublishService.class);
	}
}
