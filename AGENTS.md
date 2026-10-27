# AGENTS.md

## CLI contract synchronization

Any REST API change in this repository—endpoint, path, method, request/response DTO, JSON field, status code, or behavior—requires a corresponding
review and update of `vempain-cli`. Keep the CLI's commands and tests aligned with the published API contract before considering the backend change complete.

## Quick orientation

- This is a **Gradle multi-project** backend: `api/` contains public REST contracts + DTOs, `service/` contains the Spring Boot implementation. Start in `settings.gradle`, `api/build.gradle`, and `service/build.gradle`.
- The runtime app is `service/src/main/java/fi/poltsi/vempain/VempainAdminApplication.java`; all REST paths are served under `/api`
  (`service/src/main/resources/application.yaml`, default port `8080`). Swagger/OpenAPI is exposed on the **management port** (`8081`).
- Java and Spring Boot versions are pinned in `gradle/libs.versions.toml` (`java`, `spring-boot`); keep them aligned with `vempain-file-backend`,
  `vempain-website-backend` and `vempain-auth`.
- Existing written guidance is in `README.md` and `docs/`.

## Architecture that matters

- The codebase is split by domain into `fi.poltsi.vempain.admin.*` and `fi.poltsi.vempain.site.*`.
  - `admin.*` = admin-owned content, auth integration, file ingest/publish orchestration.
  - `site.*` = the separate website-facing data model (`WebSiteUser`, `WebSitePage`, `WebSiteFile`, ACL mappings, site config).
- New REST work usually spans **both modules**:
  1. Add/adjust contract in `api/src/main/java/.../rest/*API.java` and DTOs in `api/src/main/java/.../api/**`.
  2. Implement the interface in `service/src/main/java/.../controller/**`.
  3. Put business logic in `service/src/main/java/.../service/**`.
  4. Add or update JPA queries in `service/src/main/java/.../repository/**`.
- URL prefixes come from `api/src/main/java/fi/poltsi/vempain/admin/api/Constants.java` (`/content-management`, `/admin-management`, `/schedule-management`).
- Controllers are intentionally thin. Example: `WebSiteManagementController` mostly authenticates, logs, and delegates to `WebSiteUserService`, `WebSiteAclService`, `WebSiteResourceService`, etc.

## Data and persistence model

- This service talks to **two PostgreSQL datasources/schemas**:
  - admin/auth datasource: configured by `AdminDatabaseConfiguration`, repositories under `fi.poltsi.vempain.admin.repository` and external auth repositories.
  - site datasource: configured by `SiteDatabaseConfiguration`, repositories under `fi.poltsi.vempain.site.repository`.
- Flyway runs separately for both DBs in `FlywayMultiDBConfiguration`.
  - Admin migrations: `service/src/main/resources/db/migration/admin`
  - Site migrations: `service/src/main/resources/db/migration/site`
  - Auth migrations (`db/migration/auth`) are also loaded on the admin Flyway; they come from the `vempain-auth-core` jar on the classpath, not from this repo,
    so their versions must never collide with `db/migration/admin`.
- When you add a persisted field, expect to update **migration + entity + request/response DTO + service mapping + tests**. Example of a recent site-field addition: `global_permission` in `V1002__add_global_permission_on_user.sql` and `WebSiteUser`.
- Every resource that is released to the website records the time of its last release in a nullable `published` column of the admin
  schema (`NULL` = never released): `page.published` (set by `PublishService.publishPageAsSystem`), `gallery.published` and
  `site_file.published` (`V1007__website_release_timestamps.sql`, set by `publishGalleryAsSystem` once the files are on the site server
  and the site rows are rewritten; an empty gallery transfers nothing and stays unpublished) and `data_store.published` (set by
  `DataService.publish` after the site table is created and filled). The DTOs expose it as `published` (`PageResponse`,
  `GalleryResponse`, `FileGroupListResponse`, `SiteFileResponse`, `DataResponse`/`DataSummaryResponse`) and the gallery list sorts
  by it (`GalleryRepositoryImpl.mapSort`). Never set it on create, update or re-ingest. Tests: `PublishServiceUTC/ITC`,
  `DataServiceUTC/ITC`, `DataCTC`, `GalleryRepositoryImplUTC`.
- The page, gallery list and site file listings take the optional `modified_after_publish` flag (`PagePagedRequest`,
  `GalleryPagedRequest` for `POST galleries/paged-list`, `SiteFilePagedRequest`): with `true` only the resources whose website copy is
  outdated are returned, i.e. `published` and `modified` are both set and `published < modified` (`fi.poltsi.vempain.tools.ReleaseState`
  for the in-memory page and gallery listings, `SiteFileSpecifications.modifiedAfterPublish()` in the site file query). A resource that
  was never released, or never modified since its creation, is not "outdated". Re-ingest from the file backend sets `modifier`/`modified`
  on the existing site file and gallery (`FileIngestService`), so a re-published file shows up in the filter until the gallery is
  published again. Tests: `ReleaseStateUTC`, `PageServiceUTC`, `GalleryServiceUTC`, `FileServiceUTC`, `SiteFileRepositoryITC`.
- Every site table a resource is published to carries the audit trail `creator`/`created`/`modifier`/`modified`, with the admin users
  named by nick like `web_site_page` (the website has its own user base, admin IDs mean nothing there): `web_site_file`,
  `web_gps_location`, `web_site_subject` and `web_site_configuration` got the nullable columns in site migration
  `V1004__site_audit_columns.sql`, and `V1005__gallery_audit_nicks.sql` turned the admin user IDs of `web_site_gallery` into nullable
  nick columns (`Gallery.getSiteGallery` no longer copies them, the publish sets them). Never store an admin user ID in a site table.
  `PublishService.publishGalleryAsSystem` copies the site file's own trail (nicks resolved with
  `nickOf`) and attributes the GPS location and subject rows it creates or updates to the file's modifier, else creator (`actor`:
  creator/created on a new site row, modifier/modified on an existing one, `WebSiteSubjectService.saveAllFromAdminSubject(subjects,
  actor)`); `WebSiteConfigurationService.updateConfiguration` sets modifier/modified to the calling administrator's nick and now.
  Rows published before the columns existed keep NULL until republished; never backfill them with made-up values. The website backend
  maps the fields read-only. Tests: `PublishServiceUTC`, `WebSiteSubjectServiceUTC`, `WebSiteConfigurationServiceUTC`.
- `fi.poltsi.vempain.admin.service.DataService` is the bridge between Admin-managed dataset metadata and the site datasource tables. GPS embed discovery depends on `findAll(type, identifierPrefix, search)` returning `time_series` rows found by `search=gps`, including legacy unprefixed identifiers.
- When Admin publishes CSV into the site datasource, column coercion is schema-driven. Timestamp/date/time columns now accept ISO text inputs and fail with `400 BAD_REQUEST` when parsing is invalid; preserve that behavior when changing publish/import logic.

## Project-specific conventions

- JSON DTOs commonly use **snake_case** via Jackson naming annotations even when Java fields are camelCase. Example: `WebSiteUserRequest` / `WebSiteUserResponse`.
- JSON API fields are mandatory snake_case across this repo; do not introduce camelCase JSON keys in DTO annotations, request/response payloads, or API docs.
- Prefer Lombok annotations for applicable Java boilerplate such as constructors, accessors, builders, and logging, unless they obscure behavior or conflict
  with framework requirements.
- Prefer Jackson v3 `tools.jackson.databind.*` naming/mapper APIs for DTO JSON behavior; keep non-`tools.jackson` annotations only when there is no
  `tools.jackson` equivalent available in current dependencies.
- Entities often provide `toResponse()` helpers; keep response mapping close to the entity when the repo already follows that pattern (see `WebSiteUser`).
- Preserve request parameter normalization already present in services instead of moving it into controllers. Example: `FileService.findAllSiteFilesAsPageableResponseFiltered()` normalizes filter columns and remaps sort properties in `sanitizePageable()`.
- Security is mostly delegated to the external `vempain-auth` packages, but local controllers still explicitly call `accessService.checkAuthentication()` and write paths use `accessService.getValidUserId()` for audit fields.
- Authorization is resource-based ACL authorization, never role-based. The ACL-linked resources are the entities extending
  `AbstractVempainEntity`: `Component`, `Form`, `Layout`, `Page`, `Gallery`, `SiteFile` (plus `UserAccount`/`Unit` from `vempain-auth`).
  Services check the caller's user/unit ACL rows through `AccessService` (`hasRead/Modify/Create/DeletePermission`, `readableSpecification()`
  for paged listings). Entities without an ACL link (`DataEntity`, `Subject`, `Language`, `PublishSchedule`, `ScanQueueSchedule`, `FileThumb`,
  `GpsLocation`, the `site.*` publishing tables) must not be ACL-checked; administration endpoints for them use `accessService.checkAdminAccess()`,
  which is the modify privilege on the reserved administrator ACL (`Constants.ADMIN_ID`), not a role. Do not add `hasRole`/`ROLE_*`/
  `@PreAuthorize("hasRole(...)")` rules.
- `AccessService` has no test-mode bypass (the former `vempain.test` flag is gone). Checks fail closed: an `acl_id` that is not positive or has
  no ACL rows is denied. `AclConsistencySchedule` repairs such entities by creating an ACL with all privileges for the entity's `creator`.
- `SiteFile` has its own ACL: site-file listings use `AccessService.readableSpecification()` and `GalleryService` attaches only the files the
  user may read, even inside a readable gallery.
- Page-gallery links (`page_gallery`, `PageGalleryService`) are derived data, not edited by hand: the admin frontend takes the `gallery`
  and hero `carousel` embeds the rich-text editor reports and sends their gallery IDs as `PageRequest.gallery_ids` (body order; null keeps
  the existing links, an empty list clears them). `PageService.saveFromPageRequest`/`updateFromRequest` store them in the same transaction
  through `PageGalleryService.setPageGalleries`, which needs modify on the page and on every gallery (a denied gallery fails the page save
  with 403). `V1006__page_gallery_from_embeds.sql` rebuilt the links of existing pages from their bodies once. There is no separate
  endpoint for setting page galleries any more; the GET `galleries/page/{id}[/list]` endpoints remain for the publish screen.
- Publishing is authorized as a whole: `PublishService.publishPage` needs modify on the page, read on its form, layout and components, and
  the right to publish every attached gallery (modify on the gallery, read on each of its files); `publishGallery` needs modify on the gallery
  and read on each file. `publishAll*`/`publishSelectedGalleries` skip items the user may not publish. The `*AsSystem` variants skip the
  checks and are reserved for `PublishItemSchedule`, which runs schedules that were authorized when they were created; never call them from
  controllers.
- ITCs run authenticated as the Flyway-seeded administrator (`AbstractITCTest.setUp`, `authenticateAs(userId)` to switch user) and
  `TestITCTools.generateAcl` grants the administrator on every generated entity; use `generateAclForOwnerOnly` for negative cases. Every
  ACL-dependent path needs both a granted and a denied test (`AccessServiceITC`, `AccessServiceUTC`, `PublishServiceITC/UTC`).
- User, unit and ACL management (`/content-management/users|units|acls`) is not implemented here: the REST contracts (`UserAPI`,
  `UnitAPI`, `AclAPI` in `vempain-auth-api`) and the controllers (`fi.poltsi.vempain.auth.controller.{User,Unit,Acl}Controller` in
  `vempain-auth-core`) are hosted through component scanning and guarded by `AdministrationGuard` (modify on the administrator ACL).
  Units nest (`unit_unit`, auth migration `V2__unit_members.sql`); `UnitService.updateMembers` rejects circular nesting and
  `UserDetailsServiceImpl` expands a principal's units to every containing unit, which `AccessService` relies on. Do not add local
  copies of these endpoints; `HostedManagementITC` checks that the shared controllers work on the admin datasource.
- File ingest (`POST /content-management/file/site-file`, `FileIngestService`) requires `FileIngestRequest.file_id`, the `files.id` of the
  source file in the file backend; it is stored as `site_file.file_id` on create and re-ingest so a site file can be traced back to its
  source (a missing or non-positive ID answers `400`, never fall back to `0`). The ingest can grant additional admin users and units privileges on the site file
  and gallery it creates or updates: `FileIngestRequest.acls` (`FileIngestAclRequest`: exactly one of `user_id`/`unit_id` + the four `*_privilege` flags).
  `validateAcls` runs inside the request validation before anything is written (every entry names either an existing, `ACTIVE`, unlocked
  user or an existing unit, each grantee at most once, and grants at least one privilege; an invalid list answers `400`), and `grantAdditionalAcls` adds the
  rows to the
  ACL of the site file and of the gallery right after the owner row is created (new resource) or merges them into the existing ACL (re-ingest: an existing row
  of the same user or unit gets the requested privileges, the ingesting account is skipped because it already holds
  everything). The grantable users are served by `GET /content-management/file/site-file/users` (`FileIngestUserResponse`: id, login
  name, name, nick only; `ACTIVE` and unlocked accounts) and the grantable units by `GET /content-management/file/site-file/units`
  (`FileIngestUnitResponse`: id, name, description) for the file backend, whose own user base is separate; the listings are on the API token
  allowlist (`ApiTokenAuthenticationFilter`) and every endpoint needs the administrator ACL like the ingest itself. The answer of an ingest
  (`FileIngestResponse`, snake_case since the ACL lookup release, the former camelCase keys are still read through `@JsonAlias`) carries the
  `site_file_id`, the `gallery_id` and `gallery_site_file_ids`, the IDs of every site file linked to the gallery after the ingest in gallery
  order, so the file backend can record what a publish created. The current grantees of an earlier ingest are read back with
  `GET /content-management/file/site-file/{siteFileId}/acls` (`FileIngestSiteFileAclResponse`: site file ID, source `file_id`, `acls`),
  `GET /content-management/file/site-file/by-file-id/{fileId}/acls` (same answer keyed by the unique `site_file.file_id`, for files published
  before the file backend recorded the site file ID) and `GET /content-management/file/gallery/{galleryId}/acls`
  (`FileIngestGalleryAclResponse`: gallery ID, shortname, `site_file_ids` in gallery order, `acls`); the `acls` rows (`FileIngestAclResponse`,
  the shape of `FileIngestAclRequest`) are the ACL rows other than the calling account's own, so a client can show them and send them back
  unchanged (`FileIngestService.additionalGrantees`); an unknown ID answers `404`. All three are on the API token allowlist. Tests:
  `FileIngestServiceUTC`, `FileIngestServiceITC` (real ACL rows), `FileIngestCTC`, `FileIngestControllerUTC`, `FileIngestAclContractJTC`
  (api module), `ApiTokenAuthenticationFilterUTC` (allowlist).
- `GET /content-management/pages/path-suggestion?prefix=` (`PageService.suggestPath`) backs the path auto-completion of the page editor:
  it matches readable pages with `PageRepository.findByPagePathPrefix(LikePatterns.prefix(prefix))` (escaped LIKE, explicit `ESCAPE`),
  drops the last part of every matching path and answers the common leading parts (`commonParentPath`, null when none). Tests:
  `PageServiceUTC`, `PageServiceITC` (ACL-denied pages excluded, wildcards literal), `PageControllerUTC`.
- Service-to-service undo endpoints used by the file backend to revert cancelled background tasks: `DELETE /content-management/file/site-file/{id}`
  (`FileIngestService.deleteIngestedSiteFile`: stored file, gallery links, subjects, thumbnail row, ACL and the `SiteFile`) and
  `DELETE /content-management/data/{identifier}` (`DataService.delete`: drops the published site table and the data set). Both require the
  administrator ACL like the ingest endpoint.
- Long-running actions run as background tasks of the shared durable task facility (`vempain-common-core`, `fi.poltsi.vempain.common.task`:
  `TaskRunner`, `TaskProgressStore`, `TaskController` = `TaskAPI` at `/api/tasks`, `202 TaskAcceptedResponse` + polled
  `TaskProgressResponse`, cooperative cancel). This service hosts it with `fi.poltsi.vempain.admin.task.AdminTaskCommandExecutor` and
  `fi.poltsi.vempain.admin.api.TaskTypeEnum`: `PUBLISH_PAGE`, `PUBLISH_ALL_PAGES`, `PUBLISH_GALLERY`, `PUBLISH_ALL_GALLERIES`,
  `PUBLISH_SELECTED_GALLERIES` (`PublishService.*AsTask` submit, `publishPageNow`/`publishPagesNow`/`publishGalleriesNow` run through the
  transactional proxy, result `{"published", "skipped"}` or `{"site_page_id"}`). The publish tasks count the smallest items: one step per
  file transferred to the site server (`JschClient.Connection.transferFilesToSite` sends every site file together with its thumbnails and calls back
  after each one, `publishGalleryAsSystem(id, progress)` advances there), one step for a gallery without files, and for a page one step
  for the page itself plus the files of its galleries (`pagePublishSteps`/`galleryPublishSteps`). The totals are computed at submit time
  so that `total_steps` is right from the first poll, and an item that fails as a whole marks its remaining steps as failed (`failRemainingSteps`). Progress
  writes commit in their own transaction (`vempain-common` 1.0.4+), so the steps are visible while the
  gallery's transaction is still open. Every task that transfers files to the site server (the page and gallery publish types) is submitted
  into the `site-sftp` task lane (`PublishService.siteTransferLane`, `TaskLane` of `vempain-common` 1.0.9+, columns `task_record.lane` and
  `lane_limit` from `V1010`): at most `vempain.site.sftp-concurrency` (`VEMPAIN_SITE_SFTP_CONCURRENCY`, default 1) of them run at the same
  time, each over an SFTP connection of its own (`JschClient.connect` returns a `JschClient.Connection` owned and closed by the publish; the
  bean itself holds no connection state, so publishes may overlap safely), while the other publish tasks wait in submit order and unrelated
  tasks keep using the remaining workers. The site database may be reached over a WireGuard tunnel that drops now and then, so the site
  writes of one publish item (the page row and its subjects in `writeSitePage`, the gallery row and its files) run in one site transaction
  through `site.service.SiteDatabaseRetry` (`execute(what, work)`: `REQUIRES_NEW` on `siteTransactionManager`, retried after a lost
  connection, recognised by `isConnectionFailure` from the cause chain, with the site pool soft-evicted and a doubling pause,
  `vempain.site.database.retry-attempts|retry-backoff-ms`); the admin side is only marked published after the site writes succeeded. For
  that to work the site services that write site tables bind to `@Transactional("siteTransactionManager")`, never to the primary admin
  manager, so a site failure does not mark the admin transaction rollback-only before the retry. The site Hikari pool pings idle
  connections (`keepalive-time`), recycles them (`max-lifetime`) and the driver uses `tcpKeepAlive`/`socketTimeout`
  (`spring.site-datasource.configuration` in `application.yaml`). `REFRESH_ALL_GALLERY_FILES`
  (`FileService.refreshAllGalleryFilesAsTask`, result `RefreshResponse`) and `PUBLISH_DATA_SET` (`DataService.publishAsTask`, result
  `DataResponse`). The immediate publish endpoints answer `202` with the task inside `PublishResponse.task` (`RefreshResponse.task` for the
  refresh, a bare `TaskAcceptedResponse` for data sets); scheduling a publish still answers `200` without a task, and the synchronous
  `publish*`/`*AsSystem` methods remain for `PublishItemSchedule`. Authorization (`canPublishPage`/`canPublishGallery`) is evaluated
  synchronously before a task is submitted and unauthorized items are skipped at submit time. Publishing has no compensations: a cancelled
  publish stops between items and leaves the published ones on the site. Task tables live in the admin datasource (`V1004__task_tables.sql`, reference schema in
  `vempain-common-core`); `AdminDatabaseConfiguration` scans
  `fi.poltsi.vempain.common.task.{entity,repository}`, `WebSecurityConfig` permits `/tasks/**` for authenticated users and `vempain.tasks.*`
  configures workers, polling, leases and retention. Tests: `TaskCTC` (hosted `/tasks` API), `AdminTaskCommandExecutorUTC`,
  the task cases of `PublishServiceUTC` (synchronous `TaskRunner` + mocked `ApplicationContext` proxy) and `DataCTC` (end to end task).
- Service-to-service calls from the file backend authenticate with an API token, not a login: `ApiTokenAPI`
  (`/admin-management/api-tokens`, administrator ACL, list/create/delete) and `ApiTokenService` generate a 256-bit `vat_...` token
  whose SHA-256 hash alone is stored (`api_token`, `V1005__api_tokens.sql`); each token carries a description, a mandatory future
  expiry and an IPv4/IPv6 network (`fi.poltsi.vempain.tools.NetworkMatcher`, a plain address is /32 or /128) and is bound to the
  administrator who created it. `ApiTokenAuthenticationFilter` (registered through the auth library's `additionalAuthenticationFilters`
  hook, before the JWT filter) reads `Constants.API_TOKEN_HEADER` (`X-Vempain-Api-Token`), checks hash, expiry and the request's
  remote address against the network (Tomcat's `forward-headers-strategy: native` resolves it behind Traefik), authenticates the
  request as the owner with the extra `API_TOKEN` authority, and only for the service endpoints in its `ALLOWED` list (site file
  ingest/delete/users, paged site files, data set CRUD); anything else answers 403, an invalid token 401. The network of a new token is
  proposed by `GET /admin-management/api-tokens/default-network` (`ApiTokenService.defaultNetwork`): with
  `vempain.admin.api-token.private-network=true` (compose/swarm `ENV_VEMPAIN_PRIVATE_NETWORK`) `PrivateNetworkDetector` lists the
  private networks of this process's own interfaces (the shared bridge/overlay networks) and the first one is pre-filled, every one is
  offered; `vempain.admin.api-token.default-network` overrides it for deployments spanning hosts; otherwise the administrator enters
  it. Tests: `ApiTokenCTC`, `ApiTokenServiceUTC`, `NetworkMatcherUTC`, `PrivateNetworkDetectorUTC`, `ApiTokenContractJTC`. Never read the
  token from `Authorization`.
- Application metrics are Micrometer meters pushed as OTLP/HTTP to the monitoring environment's Prometheus (`micrometer-registry-otlp`,
  push only: nothing is exposed on the management port for scraping because `/actuator` is routed publicly through Traefik). The
  container environment `VEMPAIN_METRICS_ENABLED|URL|USERNAME|PASSWORD|CERTIFICATE|STEP|ROLE` is mapped to `vempain.metrics.*` in
  `application.yaml`; `vempain-common` core turns those into the Boot `management.otlp.metrics.export.*` properties (basic-auth header,
  SSL bundle for the self-signed certificate) with its `MetricsExportEnvironmentPostProcessor`, so this backend needs `vempain-common`

  > = the release that ships it (bump `gradle/libs.versions.toml`; the shared `vempain.tasks.*` meters come from the same release). Every
  > meter carries the `application`, `role` (`api`/`scheduler`) and `instance` tags. The custom meters live in
  > `fi.poltsi.vempain.admin.metrics`: `AdminMetrics` (the counters and timers: `vempain.api_token.requests{result}`,
  > `vempain.ingest.files{result}`/`vempain.ingest.bytes`, `vempain.publish_schedule.items{result}`/`vempain.publish_schedule.lag`,
  > `vempain.consistency.fixed{schedule}`, and the `cachedGauge` helper that re-reads a database value at most once a minute) and
  > `AdminInventoryMetrics` (the gauges `vempain.inventory.pages|galleries|site_files|subjects`, `vempain.publish_schedule.pending`,
  > `vempain.api_token.expiry.days`). `JschClient` measures the SFTP transfers on the global registry (`vempain.site_transfer.files{kind}`, `.bytes`,
  > `.duration{result}`, `.connect.failures`). The two Hikari pools are named `admin` and
  > `site` (`spring.*-datasource.configuration.pool-name`) so the `hikaricp.*` meters are distinguishable, and the Tomcat MBeans are
  > registered for `tomcat.*`. Report new events through `AdminMetrics` (one place for names and tags, low-cardinality tags only); tests
  > use a `SimpleMeterRegistry` (`AdminMetricsUTC`, `AdminInventoryMetricsUTC`, the metric assertions of the filter, schedule and
  > `JschClientUTC` tests).

- Internal links of page content: `GET /admin-management/site/links?search=&limit=` (`WebSiteManagementAPI.findLinkTargets`,
  `WebSiteResourceService.findLinkTargets`, `InternalLinkResponse` with `type` PAGE|SITE_FILE, `title`, `path`) lists the published pages (title or path
  contains the text) and then the published files (path contains it), each sorted and limited (1..50). It reads the site
  schema only, so nothing unpublished can be offered, and needs a signed-in user, not an administrator. The admin SPA maps the targets to
  website URLs (`/pages/<path>`, `/file/<path>`) and hands them to the rich-text editor's `findInternalLinks` provider.
- Page tags: a page carries subjects like a site file does (`page_subject`, `V1009`, `SubjectRepository.getSubjectsByPageId`). `PageRequest.tags`
  (names; null keeps the existing tags, an empty list removes them) is applied by `PageService.saveFromPageRequest`/`updateFromRequest` through
  `SubjectService.replacePageSubjects` (trimmed, deduplicated case-insensitively, unknown names become new `subjects` rows), and
  `PageService.populateResponse` returns them alphabetically as `PageResponse.tags`. `PublishService.publishPageAsSystem` publishes them with
  the page: the tag blacklist is applied, the subjects are upserted on the site (`WebSiteSubjectService.saveAllFromAdminSubject`, attributed to
  the page's modifier, else creator) and `web_site_page_subject` is rewritten (`replaceSitePageSubjects`). The website backend already serves them
  as the page's `subjects`. Tests: `PageServiceUTC`, `PageServiceITC`, `SubjectServiceUTC`, `PublishServiceUTC`.
- Tag blacklist: `TagBlacklistAPI` (`GET|POST /admin-management/tag-blacklist`, `DELETE .../{entry_id}`, administrators only via
  `accessService.checkAdminAccess()`; `TagBlacklistRequest`/`TagBlacklistResponse`) is backed by `TagBlacklistController`, `TagBlacklistService`,
  `TagBlacklistRepository` and the `tag_blacklist` table (`V1008`, unique on `LOWER(tag_name)`, no ACL: it is configuration like subjects). A
  blacklisted tag never reaches the website: `PublishService.publishGalleryAsSystem` takes one `TagBlacklist` snapshot (`TagBlacklistService.snapshot()`) per
  publish and `publishGalleryFile` applies it to every file, leaving blacklisted subjects out of
  `WebSiteSubjectService.saveAllFromAdminSubject` (`filterSubjects`) and removing them from the keyword lists of the site file's metadata JSON (`stripMetadata`:
  `Keywords`, `Subject`, `HierarchicalSubject`, `WeightedFlatSubject`, ... in any group; free text such as captions is never
  touched). Matching is case-insensitive on the trimmed name and a hierarchical tag (`People|Family`) matches on any level. Pages carry no
  subjects of their own today; their galleries are cleaned by the same code when the page publish publishes them. Blacklisting changes nothing on
  the site until a resource is published again. Tests: `TagBlacklistUTC`, `TagBlacklistServiceUTC`, `TagBlacklistControllerUTC`,
  `TagBlacklistCTC`, the gallery publish case of `PublishServiceUTC`.
- Resource statistics: `StatisticsAPI` (`GET /admin-management/statistics`, `AdminStatisticsResponse` with `StatisticsCount`,
  `StatisticsPublishState`, `StatisticsMonthlyCount`, `StatisticsUserCount`) is implemented by `StatisticsController` and `StatisticsService`,
  which counts pages, galleries, site files, components, forms and layouts with fixed native aggregate queries (`EntityManager`, tables listed in
  `StatisticsService.RESOURCES`): totals, site files by type, published/not published, created and published per UTC month, creators (joined
  with `user_account` for the nick), pages per form, forms per layout and component, pages per parent path and site files per path. The numbers
  are system wide and deliberately not ACL filtered, like the inventory metrics; the admin frontend shows them on its Statistics screen. Tests:
  `StatisticsServiceUTC` (canned rows), `StatisticsServiceITC` (real schema), `StatisticsControllerUTC`, `StatisticsCTC`.

  > The OTLP push reports itself through `MeteredOtlpMetricsSender.Registrar` (a `BeanPostProcessor` that decorates the
  > `OtlpMetricsSender` Boot builds, never replaces it) and `MetricsExportMetrics` (`vempain.metrics_export.requests{result=success|rejected|error,status}`,
  > `.duration{result}`, `.bytes`, the gauges `.last_success.age.seconds` (-1 before the first success) and `.meters`); the file backend carries the same
  > two classes and the monitoring dashboard `vempain-application.json` shows them in its "Metrics export" row.

- Distributed tracing: `spring-boot-starter-opentelemetry` (Micrometer Tracing with the OpenTelemetry bridge and the OTLP exporter). An
  incoming W3C `traceparent` (Traefik, the admin SPA, the file backend's Feign calls) is continued; Boot puts `traceId`/`spanId` into the
  MDC and the console pattern, so every log line of a request carries them. `VEMPAIN_TRACING_ENABLED|URL|SAMPLING` map to
  `vempain.tracing.*` and `management.tracing.enabled` in `application.yaml`; the Tempo endpoint, the basic-auth header, the certificate
  bundle and the sampling probability (`management.opentelemetry.tracing.export.otlp.*`, `management.tracing.sampling.probability`) are
  set by `vempain-common`'s `MetricsExportEnvironmentPostProcessor`, reusing the metrics credentials, so the `vempain-common` bump above
  covers tracing too. `management.tracing.export.enabled` is bound to the same switch, because Boot's `LogCorrelationEnvironmentPostProcessor`
  adds the `[app,traceId,spanId]` block to the console pattern whenever a `Tracer` is on the classpath and the export is not disabled;
  the block therefore appears only when tracing is on, identically in the three backends. Background tasks run in a span of their own (`task <type>`,
  `vempain-common` `TaskRunner`). Keep tracing off in
  tests (`vempain.tracing.enabled` defaults to false); `TraceContinuationCTC` switches it on for one context and asserts the continuation
  of a caller's trace id into the server span and the MDC. Never log a trace id as a request parameter or label it with addresses.
- Security findings and their mitigations are recorded in `security/OWASP-2025-audit-report.md`; keep it current when changing authorization,
  query building, SSH/filesystem publishing or input handling.
- `lombok.config` sets `lombok.addLombokGeneratedAnnotation = true` so generated code is excluded from JaCoCo; keep it when touching coverage settings.
- The repo uses **4 spaces** for Java indentation and a 160-char line length (`.editorconfig`; the layout beyond that is owned by
  `.config/eclipse-formatter.xml`). Avoid mass reformatting.

## Migration scripts

- `migration-scripts/<topic>/` holds one-off data migrations that run outside the service (bash or Python, each with a `README.md`
  and, for Python, standard-library `unittest` tests runnable without a database). They never ship in the jar. The first one,
  `migration-scripts/website-release-timestamps/`, backfills the `published` columns of pages, galleries, site files and data sets
  from the website: exact release times where the site has them (`web_site_page.published`, the files' modification times on the site
  server, PostgreSQL commit timestamps when tracked) and otherwise a comparison of the admin copy with the site copy (unchanged =
  released at the last modification, changed = the newest known time older than it, else unresolved and reported). It writes a report
  and an SQL file and only updates NULL columns unless told to overwrite. `populate_thumbnail_path.sh` at the repository root predates
  the directory and stays where it is.

## External integrations

- GitHub Packages dependencies are required for builds: `vempain-auth-*` and `vempain-common-api`. Build/publish uses `gpr.user` / `gpr.token` or
  `GITHUB_ACTOR` / `GITHUB_TOKEN`.
  This repository must never depend on `vempain-file-backend-api`: the file backend depends on this API (it calls the admin backend), so a
  dependency in the other direction is a cycle. Types both backends need (`FileTypeEnum`, `TagRequest`, `CopyrightRequest`,
  `LocationRequest`/`LocationResponse`) come from `vempain-common` (`fi.poltsi.vempain.common.api.*`); add new shared types there, release it and
  bump `vempain-common` in `gradle/libs.versions.toml`. A release of this API is consumed by the file backend, so release this repository before it.
- File publishing is a core feature, not an afterthought:
  - `PublishService` orchestrates site-side publishing.
  - `JschClient` pushes converted files + thumbs over SFTP to the remote site root.
  - `exiftool` must exist (`vempain.exiftool.path`, an absolute path checked by `SetupVerification`; `ENV_VEMPAIN_ADMIN_EXIFTOOL_PATH` in the
    cluster `.env`). It runs only through the shared `fi.poltsi.vempain.common.metadata.ExifToolService` of `vempain-common` (persistent
    `-stay_open` processes): `ImageTools.resizeImage` copies the tags of the source into the thumbnail with `copyAllTags` (a failure is logged, the
    thumbnail stays usable) and `TestITCTools` reads site files with `readMetadataJson` + `MetadataJson.extractMimetype`. The former local
    `MetadataTools`/`JsonTools` are gone; never add a `ProcessBuilder("exiftool")` here again.
- `prod` is the default profile (`spring.profiles.default`). In it `SetupVerification.verifyProductionDatasources` refuses to start unless both
  `spring.admin-datasource.url` and `spring.site-datasource.url` require TLS (`sslmode=require`, `verify-ca` or `verify-full`, or `ssl` without an
  `sslmode`). The tests set `spring.profiles.default=test` in `src/test/resources/application.properties` so Testcontainers URLs are not checked.
  The compose files start PostgreSQL through `postgres-ssl-entrypoint.sh` (a copy of the `vempain-cluster` script; keep them identical), which
  rejects plaintext connections.

## Workflows agents should actually use

- Local DB bootstrap for dev: `docker_db.sh` starts **two** Postgres containers on ports **5433** and **5434** matching `start.sh`.
- Fast local start with working ports/args: use `./start.sh` (it runs `bootRun` with `server.port=9090`, `management.server.port=9091`, local DB URLs, SSH/test paths).
- Standard build/test entry points:
  - `./gradlew clean test`
  - `./gradlew :service:bootJar`
  - `./gradlew :service:bootRun --args='...'`
- Integration-test environment is unusual: `testSetup.sh --developer-name <login>` must be run as root to create local users, SSH keys, and directories consumed by tests; `testCleanup.sh` removes them.

## Testing patterns

- Test suffixes are meaningful and shared across the Vempain Java repos:
  - `*UTC` = unit tests with Mockito
  - `*ITC` = integration tests with Spring Boot + Testcontainers
  - `*CTC` = controller tests (MockMvc on top of the integration context, e.g. `LayoutCTC`, `WebSiteManagementCTC`)
  - `*JTC` = JSON contract tests for the `api` module (e.g. `RestContractJTC`)
  - Helper classes without a suffix (`AbstractITCTest`, `TestConfig`, `Test*Tools`, `Mock*Tools`) are not test classes.
- `AbstractITCTest` is the integration-test backbone: it starts two Postgres containers, runs **Flyway clean+migrate before each test**, and recreates filesystem directories under `/var/tmp`.
- If you change schema, repository behavior, or filesystem/publish logic, add/update both a focused `UTC` and the relevant `ITC` when feasible.
- After every code modification, run the relevant tests for the touched modules and report the results in the response.
- GPS data-set listing behavior is integration-tested in `service/src/test/java/fi/poltsi/vempain/admin/service/DataServiceITC.java`; keep selector-query changes covered there.
- Every `LIKE` pattern built from request text goes through `fi.poltsi.vempain.tools.LikePatterns` (`contains`/`containsIgnoreCase`/`prefix`: escapes
  `%`, `_` and `\\`, truncates to 200 characters; `limitTokens` caps a search at 10 tokens) and declares the escape character,
  as in `GalleryRepositoryImpl` (native SQL, `ESCAPE_CLAUSE`) and `SiteFileSpecifications` (Criteria API, `ESCAPE_CHAR`). Request text never acts as wildcard
  syntax (OWASP A05).

## Container image

- `Dockerfile` is runtime-only (the CI-built jar is added as `/app.jar`) and starts `container-entrypoint.sh` as root: it builds the private
  JVM truststore from the mounted admin certificate, gives the image's `vempain` account the ids `VEMPAIN_UID`/`VEMPAIN_GID` (default
  `6666`, set per container in the `vempain-cluster` stacks as `ENV_VEMPAIN_UID`/`ENV_VEMPAIN_GID`) by rewriting `/etc/passwd` and
  `/etc/group`, and runs Java through `su-exec` as those ids; a container started as a non-root user keeps that user. The ids must be the
  host owner of the mounted `/vempain_admin/site-files` and `/vempain_admin/vempain/.ssh`. The same script, apart from the truststore, is in the website
  backend; keep the user handling of
  the three identical.

## Tag ACL rule

Tags are metadata, not ACL-bearing resources. Tag entities have no ACL information, so tag list, search, and mutation endpoints must not perform ACL checks on
tags. ACL checks apply only to resources that explicitly carry an ACL.
