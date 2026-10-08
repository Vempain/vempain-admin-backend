# AGENTS.md

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
- Publishing is authorized as a whole: `PublishService.publishPage` needs modify on the page, read on its form, layout and components, and
  the right to publish every attached gallery (modify on the gallery, read on each of its files); `publishGallery` needs modify on the gallery
  and read on each file. `publishAll*`/`publishSelectedGalleries` skip items the user may not publish. The `*AsSystem` variants skip the
  checks and are reserved for `PublishItemSchedule`, which runs schedules that were authorized when they were created; never call them from
  controllers.
- ITCs run authenticated as the Flyway-seeded administrator (`AbstractITCTest.setUp`, `authenticateAs(userId)` to switch user) and
  `TestITCTools.generateAcl` grants the administrator on every generated entity; use `generateAclForOwnerOnly` for negative cases. Every
  ACL-dependent path needs both a granted and a denied test (`AccessServiceITC`, `AccessServiceUTC`, `PublishServiceITC/UTC`).
- Service-to-service undo endpoints used by the file backend to revert cancelled background tasks: `DELETE /content-management/file/site-file/{id}`
  (`FileIngestService.deleteIngestedSiteFile`: stored file, gallery links, subjects, thumbnail row, ACL and the `SiteFile`) and
  `DELETE /content-management/data/{identifier}` (`DataService.delete`: drops the published site table and the data set). Both require the
  administrator ACL like the ingest endpoint.
- Long-running actions run as background tasks of the shared durable task facility (`vempain-common-core`, `fi.poltsi.vempain.common.task`:
  `TaskRunner`, `TaskProgressStore`, `TaskController` = `TaskAPI` at `/api/tasks`, `202 TaskAcceptedResponse` + polled
  `TaskProgressResponse`, cooperative cancel). This service hosts it with `fi.poltsi.vempain.admin.task.AdminTaskCommandExecutor` and
  `fi.poltsi.vempain.admin.api.TaskTypeEnum`: `PUBLISH_PAGE`, `PUBLISH_ALL_PAGES`, `PUBLISH_GALLERY`, `PUBLISH_ALL_GALLERIES`,
  `PUBLISH_SELECTED_GALLERIES` (`PublishService.*AsTask` submit, `publishPageNow`/`publishPagesNow`/`publishGalleriesNow` run through the
  transactional proxy, result `{"published", "skipped"}` or `{"site_page_id"}`), `REFRESH_ALL_GALLERY_FILES`
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
- Security findings and their mitigations are recorded in `security/OWASP-2025-audit-report.md`; keep it current when changing authorization,
  query building, SSH/filesystem publishing or input handling.
- `lombok.config` sets `lombok.addLombokGeneratedAnnotation = true` so generated code is excluded from JaCoCo; keep it when touching coverage settings.
- The repo uses **tabs** for Java indentation and a 160-char line length (`.editorconfig`). Avoid mass reformatting.

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
  - `exiftool` must exist; startup will fail fast in `SetupVerification` if required paths/files are missing.

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

## Tag ACL rule

Tags are metadata, not ACL-bearing resources. Tag entities have no ACL information, so tag list, search, and mutation endpoints must not perform ACL checks on
tags. ACL checks apply only to resources that explicitly carry an ACL.
