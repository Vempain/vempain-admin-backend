# AGENTS.md

## Quick orientation
- This is a **Gradle multi-project** backend: `api/` contains public REST contracts + DTOs, `service/` contains the Spring Boot implementation. Start in `settings.gradle`, `api/build.gradle`, and `service/build.gradle`.
- The runtime app is `service/src/main/java/fi/poltsi/vempain/VempainAdminApplication.java`; all REST paths are served under `/api` (`service/src/main/resources/application.yaml`). Swagger/OpenAPI is exposed on the **management port**.
- Existing written guidance is in `README.md`; there were no other agent-specific instruction files in the repo when this file was generated.

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
  - Auth migrations are also loaded on the admin Flyway (`db/migration/auth`).
- When you add a persisted field, expect to update **migration + entity + request/response DTO + service mapping + tests**. Example of a recent site-field addition: `global_permission` in `V1002__add_global_permission_on_user.sql` and `WebSiteUser`.

## Project-specific conventions
- JSON DTOs commonly use **snake_case** via Jackson naming annotations even when Java fields are camelCase. Example: `WebSiteUserRequest` / `WebSiteUserResponse`.
- Entities often provide `toResponse()` helpers; keep response mapping close to the entity when the repo already follows that pattern (see `WebSiteUser`).
- Preserve request parameter normalization already present in services instead of moving it into controllers. Example: `FileService.findAllSiteFilesAsPageableResponseFiltered()` normalizes filter columns and remaps sort properties in `sanitizePageable()`.
- Security is mostly delegated to the external `vempain-auth` packages, but local controllers still explicitly call `accessService.checkAuthentication()` and write paths use `accessService.getValidUserId()` for audit fields.
- The repo uses **tabs** for Java indentation and a 160-char line length (`.editorconfig`). Avoid mass reformatting.

## External integrations
- GitHub Packages dependencies are required for builds: `vempain-auth-*` and `vempain-file-backend-api`. Build/publish uses `gpr.user` / `gpr.token` or `GITHUB_ACTOR` / `GITHUB_TOKEN`.
- File publishing is a core feature, not an afterthought:
  - `PublishService` orchestrates site-side publishing.
  - `JschClient` pushes converted files + thumbs over SFTP to the remote site root.
  - `exiftool` must exist; startup will fail fast in `SetupVerification` if required paths/files are missing.
- `typescript-dtos/SiteWebAccessTypes.ts` mirrors part of the site-access API for frontend consumers; update it when changing those payloads.

## Workflows agents should actually use
- Local DB bootstrap for dev: `docker_db.sh` starts **two** Postgres containers on ports **5433** and **5434** matching `start.sh`.
- Fast local start with working ports/args: use `./start.sh` (it runs `bootRun` with `server.port=9090`, `management.server.port=9091`, local DB URLs, SSH/test paths).
- Standard build/test entry points:
  - `./gradlew clean test`
  - `./gradlew :service:bootJar`
  - `./gradlew :service:bootRun --args='...'`
- Integration-test environment is unusual: `testSetup.sh --developer-name <login>` must be run as root to create local users, SSH keys, and directories consumed by tests; `testCleanup.sh` removes them.

## Testing patterns
- Test suffixes are meaningful:
  - `*UTC` = unit tests with Mockito
  - `*ITC` = integration tests with Spring Boot + Testcontainers
  - `*RTC` = REST/controller-level tests
- `AbstractITCTest` is the integration-test backbone: it starts two Postgres containers, runs **Flyway clean+migrate before each test**, and recreates filesystem directories under `/var/tmp`.
- If you change schema, repository behavior, or filesystem/publish logic, add/update both a focused `UTC` and the relevant `ITC` when feasible.

