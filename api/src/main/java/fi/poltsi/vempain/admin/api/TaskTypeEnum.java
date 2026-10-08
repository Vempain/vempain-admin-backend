package fi.poltsi.vempain.admin.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Long-running actions of the admin backend that run as background tasks of the shared task facility
 * ({@code fi.poltsi.vempain.common.task}). The facility carries the type as a string; this enum documents the types this service
 * emits and the result payload each one attaches to the finished task. Publishing cannot be reverted: a cancelled publish task
 * stops between items and leaves the already published items on the site.
 */
@Schema(description = "Background task types emitted by the admin backend",
		allowableValues = {"PUBLISH_PAGE", "PUBLISH_ALL_PAGES", "PUBLISH_GALLERY", "PUBLISH_ALL_GALLERIES", "PUBLISH_SELECTED_GALLERIES",
						   "REFRESH_ALL_GALLERY_FILES", "PUBLISH_DATA_SET"})
public enum TaskTypeEnum {
	/**
	 * Publishes one page and the galleries attached to it to the site. Result payload: {@code {"site_page_id": n}}.
	 */
	PUBLISH_PAGE,
	/**
	 * Publishes every page the caller may publish, one step per page. Result payload: {@code {"published": n, "skipped": m}}.
	 */
	PUBLISH_ALL_PAGES,
	/**
	 * Transfers the files of one gallery to the site server and publishes its metadata. Result payload: {@code {"published": 1, "skipped": 0}}.
	 */
	PUBLISH_GALLERY,
	/**
	 * Publishes every gallery the caller may publish, one step per gallery. Result payload: {@code {"published": n, "skipped": m}}.
	 */
	PUBLISH_ALL_GALLERIES,
	/**
	 * Publishes the selected galleries the caller may publish, one step per gallery. Result payload: {@code {"published": n, "skipped": m}}.
	 */
	PUBLISH_SELECTED_GALLERIES,
	/**
	 * Regenerates the derived files of every gallery, one step per gallery. Result payload: {@code RefreshResponse}.
	 */
	REFRESH_ALL_GALLERY_FILES,
	/**
	 * Creates or replaces the published table of a data set in the site database. Result payload: {@code DataResponse}.
	 */
	PUBLISH_DATA_SET
}
