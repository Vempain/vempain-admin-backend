package fi.poltsi.vempain.admin.api;

public class Constants {
	public static final Long   ADMIN_ID            = 1L;
	public static final String REST_CONTENT_PREFIX = "/content-management";
	public static final String REST_FILE_PREFIX    = REST_CONTENT_PREFIX + "/file";
	public static final String REST_DATA_PREFIX    = REST_CONTENT_PREFIX + "/data";
	public static final String REST_SCHEDULE_PREFIX = "/schedule-management";
	public static final String REST_ADMIN_PREFIX   = "/admin-management";
	public static final String LOGIN_PATH          = "/login";
	public static final String TEST_PATH_PREFIX    = "/test";
	/**
	 * Header carrying a service-to-service API token (managed under {@code /admin-management/api-tokens}); never a JWT bearer token.
	 */
	public static final String API_TOKEN_HEADER = "X-Vempain-Api-Token";

	private Constants() {
		throw new IllegalStateException("Constants class");
	}
}
