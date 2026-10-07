# OWASP Top 10:2025 Audit — Vempain Admin backend/frontend working tree

Date: 2026-09-15 · Scope: backend/API, configuration, CI boundary · Method: static review + targeted unit/integration tests

## Executive summary

The audit found one exploitable authorization bypass (the shipped `vempain.test=true` default), several authenticated-user
privilege-escalation paths, wildcard controller CORS, configuration exposure, and exception/configuration logging leaks.
The bypass and HIGH authorization issues are fixed at the controller/service boundary and covered by tests. Production now
defaults to the `prod` profile, requires secrets/database credentials from the environment, restricts actuator exposure,
and adds response hardening. The companion frontend audit fixed rich-text/iframe injection, footer HTML injection, CI
permissions, and browser/container hardening; its detailed evidence is in `vempain-admin-frontend/security/OWASP-2025-audit-report.md`.

## Inventory and threat model

* **HTTP assets:** `/api/content-management/**`, `/api/admin-management/**`, and `/api/schedule-management/**` are
  authenticated admin/content APIs. `/api/login` is supplied by the shared auth dependency. Management is on port 8081;
  production Swagger/API docs are disabled by `prod.yaml`, and the shared auth chain denies production actuator/docs.
* **Trust boundaries:** browser → Spring API/CORS; API → admin PostgreSQL and site PostgreSQL; API → File API and remote
  site over SSH/SFTP; API → local converted-file storage; CI → GitHub Packages/container registry.
* **Sensitive data:** JWT signing secret, DB credentials, SSH private key, user/website-user PII, ACLs, published content,
  and file metadata. Authorization is now required for administrator-only data/user/unit/schedule/site-management paths.
* Anonymous callers are rejected by the shared JWT filter. A stolen authenticated non-administrator token must not reach
  administrator-only paths; the fixed `AccessService.checkAdminAccess()` requires modify permission on reserved ACL 1.
  Authorized content ACL checks remain in their existing services.

## Coverage matrix

| Category                           | Checked | Findings (C/H/M/L) | Status                                       |
|------------------------------------|---------|-------------------:|----------------------------------------------|
| A01 Broken Access Control          | yes     |            0/2/0/0 | fixed                                        |
| A02 Security Misconfiguration      | yes     |            0/0/3/0 | fixed                                        |
| A03 Software Supply Chain Failures | yes     |            0/0/0/0 | recommendations below                        |
| A04 Cryptographic Failures         | yes     |            0/0/1/0 | deferred/infrastructure                      |
| A05 Injection                      | yes     |            0/0/0/0 | checked; no exploitable finding proven       |
| A06 Insecure Design                | yes     |            0/0/1/0 | fixed (bounded request/config defaults)      |
| A07 Authentication Failures        | yes     |            0/0/1/0 | deferred/shared-auth                         |
| A08 Software/Data Integrity        | yes     |            0/0/1/0 | deferred/CI                                  |
| A09 Logging & Alerting Failures    | yes     |            0/0/1/0 | fixed logging leak; alerting deferred        |
| A10 Exceptional Conditions         | yes     |            0/0/1/0 | fixed response leak; central advice deferred |

## Findings

### [HIGH] F-01 — Test-mode ACL bypass enabled by default · A01:2025 · CWE-863/CWE-639

* **Location:** `service/src/main/resources/application.yaml:1-4,98`; `service/src/main/java/fi/poltsi/vempain/admin/service/AccessService.java:53-70,107-111`.
* **Description/impact:** the common configuration enabled `vempain.test=true`; in that mode the service selected the
  first database user and granted every ACL permission. A production deployment without an explicit override therefore
  allowed any authenticated low-privilege request that reached a controller to act with test authorization.
* **Reproduction:** start without `vempain.test=false`, call an ACL-protected content endpoint, or invoke file ingest;
  `AccessService` takes the test branch and returns true/first-user identity.
* **Fix:** production default is `prod` and `vempain.test=false`; test resources explicitly opt into the test fixture.
  Docker/start scripts require runtime JWT secrets. The existing test-mode behavior is retained only for tests.
* **Verification:** `AccessServiceUTC.checkAdminAccess...`; `SecurityConfigurationUTC.productionDefaultsDoNotEnableTestBypassOrUnrestrictedActuator`;
  `./gradlew clean test` (721 service tests passed, 3 skipped).

### [HIGH] F-02 — Authenticated non-admins could mutate admin/site management data · A01:2025 · CWE-862/CWE-639

* **Location:**
  `service/src/main/java/fi/poltsi/vempain/admin/controller/{UserController,UnitController,DataController,ScheduleController,WebSiteManagementController}.java`;
  `.../controller/{AclController,file/FileIngestController,file/FileSystemController}.java`.
* **Description/impact:** these endpoints either had no local authorization or only checked authentication, while they
  expose user PII, ACLs, datasets, schedules, filesystem structure, site configuration, and mutating operations.
  Any authenticated account could therefore perform administrator actions.
* **Reproduction:** use a valid non-admin JWT against any listed endpoint; before the fix controller execution proceeded
  after `checkAuthentication()` or without a check.
* **Fix:** all listed operations call `AccessService.checkAdminAccess()`, which requires modify permission on reserved ACL
  1 and fails closed for missing sessions. Existing object/content ACL checks were preserved.
* **Verification:** `AccessServiceUTC.checkAdminAccessRejectsUserWithoutAdministratorAcl`,
  `AclControllerUTC`, `WebSiteManagementControllerUTC`, and targeted controller/integration tests; full Gradle suite passed.

### [MEDIUM] F-03 — Wildcard CORS declarations · A01/A02:2025 · CWE-942

* **Location:** API interfaces previously annotated with `@CrossOrigin(origins="*")` (Acl, Component, Form, Layout, Page,
  Unit, User APIs); centralized allowed origins are `service/src/main/resources/application.yaml:118-121`.
* **Impact:** endpoint-level wildcard declarations could bypass the configured origin allow-list and allow arbitrary web
  origins to invoke API calls.
* **Fix/verification:** removed all wildcard annotations; CORS is delegated to shared Spring Security configuration and
  the configured frontend origin. `SecurityConfigurationUTC` and full suite pass.

### [MEDIUM] F-04 — Production actuator/configuration defaults were overexposed · A02:2025 · CWE-16

* **Location:** `service/src/main/resources/application.yaml:44-76`.
* **Impact:** wildcard actuator exposure and unrestricted management access exposed operational metadata if the management
  port was reachable.
* **Fix:** only `health` and `info` are exposed; management access defaults to `none`, health is read-only, management
  remains on a separate port, and production Swagger/API docs remain disabled.
* **Verification:** `SecurityConfigurationUTC` and full suite pass.

### [MEDIUM] F-05 — Runtime secrets had committed/default fallbacks · A02/A04:2025 · CWE-260/CWE-321

* **Location:** `service/src/main/resources/application.yaml:19-28,94-97`; `docker-compose.yaml:16`; `start.sh:9`.
* **Impact:** committed DB credentials and placeholder/weak JWT values made accidental insecure deployment possible.
* **Fix:** DB URLs/credentials, frontend URL, JWT secret, and JWT expiry are environment/command-line supplied; Docker
  Compose now fails if `ENV_VEMPAIN_JWT_SECRET` is absent and the local start script requires `VEMPAIN_JWT_SECRET`
  without exporting a committed fallback secret.
  The base config still contains `override-me` for non-secret required paths and `SetupVerification` fails closed.
* **Residual:** test-only JWT material remains in `service/src/test/resources/application.properties`; it is not a
  production credential but should be replaced by generated per-run test configuration in a future cleanup.

### [MEDIUM] F-06 — Sensitive configuration values were written to debug logs · A09:2025 · CWE-532

* **Location:** `service/src/main/java/fi/poltsi/vempain/SetupVerification.java:114-149`.
* **Impact:** the prior property dump excluded only `password`/`credentials` and could log JWT secrets, tokens, private
  key paths, or API keys.
* **Fix:** sensitive property names (`password`, `secret`, `token`, `private-key`, `credentials`, `api-key`) are filtered,
  required-value verification no longer logs values, and authentication failures no longer stringify the principal.
* **Verification:** targeted security tests and full test suite passed; remaining logging should be reviewed with centralized
  structured logging.

### [MEDIUM] F-07 — Internal exception objects were returned to clients · A10:2025 · CWE-209

* **Location:** `service/src/main/java/fi/poltsi/vempain/admin/controller/{UserController,UnitController}.java`.
* **Impact:** controller exception handlers serialized exception class/message details, allowing internal implementation
  and potentially sensitive error data to escape.
* **Fix:** handlers log server-side and return an empty 500 response; tests assert no exception body.
* **Verification:** `UserAccountControllerUTC.handleRuntimeExceptionsOk`, `UnitControllerUTC.handleRuntimeExceptionsOk`,
  and full suite pass.

### [MEDIUM] F-08 — Unbounded request-body draining · A06:2025 · CWE-400

* **Location:** `service/src/main/resources/application.yaml:40-42`.
* **Impact:** `server.tomcat.max-swallow-size=-1` permitted unbounded discarded request data, increasing request-body
  exhaustion risk.
* **Fix:** capped at the configured 120 MB request limit.
* **Verification:** `SecurityConfigurationUTC` checks the production configuration; full suite passes.

### [MEDIUM] F-09 — Security response headers were not centralized · A02:2025 · CWE-693

* **Location:** new `service/src/main/java/fi/poltsi/vempain/SecurityHeadersFilter.java`.
* **Impact:** API/error responses lacked a consistent CSP, HSTS, clickjacking, MIME-sniffing, referrer, permissions, and
  cache policy.
* **Fix:** a `OncePerRequestFilter` applies these headers before controller/security responses and marks API responses
  `no-store`.
* **Verification:** `SecurityHeadersFilterUTC.appliesSecurityHeadersBeforeDelegating`; full suite passes.

## Checklist verdicts

The following records every playbook item at the requested audit level. `UNVERIFIED` is used only where evidence requires
an external deployment, shared repository, or scanner not present in this repository.

| Category | Checklist verdicts and evidence                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A01      | PASS deny-by-default/shared auth chain; PASS deliberate public login/health (shared auth); PASS content ACL checks; PASS fixed admin object authorization; PASS mutating admin paths fixed; PASS no wildcard controller CORS; PASS test-mode bypass disabled by default; PASS path traversal defenses in `FileIngestService`; UNVERIFIED SSRF/open redirect/token revocation/rate-limit behavior (shared auth or external boundary).                                                                                                                                                                                    |
| A02      | PASS prod default; PASS error stack configuration not enabled; PASS actuator restricted; PASS Swagger disabled in `prod.yaml`; PASS secrets required from environment; PASS JPA validation/show-sql off; PASS multipart/body caps; PASS security headers; PASS CORS allow-list source; PASS Docker management port separation; UNVERIFIED deployed proxy/header and filesystem-root checks.                                                                                                                                                                                                                             |
| A03      | PASS Gradle versions are pinned and private registries use HTTPS; PASS lock/build manifests are committed; UNVERIFIED dependency CVE/SBOM scan, image digest policy, CI reusable-workflow SHA pinning, branch protection, and install-script policy.                                                                                                                                                                                                                                                                                                                                                                    |
| A04      | PASS shared auth uses BCrypt cost 12 (verified from resolved dependency); PASS JWT secret no longer has application fallback; PASS `SecureRandom` used by local test tooling; PASS no custom trust-all manager found; UNVERIFIED production TLS/JDBC `verify-full`, rotation, backup encryption, and MFA.                                                                                                                                                                                                                                                                                                               |
| A05      | PASS repository queries use bind parameters/allow-listed sort fields; PASS `LIKE` patterns built from request text are escaped and length-capped through `fi.poltsi.vempain.tools.LikePatterns` with an explicit escape character and searches are limited to 10 tokens (`GalleryRepositoryImpl`, `SiteFileSpecifications`; `GalleryRepositoryImplUTC`, `LikePatternsUTC`); PASS ingest sanitizes and contains paths and checks SHA-256; PASS DTO validation exists on sensitive request fields; N/A XML/YAML untrusted parsers and browser XSS sinks (frontend/shared repos out of scope); UNVERIFIED runtime fuzzing. |
| A06      | PASS page/resource size caps in services and request size cap; PASS file ingest compensates stored file on failure; PASS test-mode fail-closed default; PASS admin operation boundary; UNVERIFIED concurrency/DB race analysis for all publish flows and anti-automation controls.                                                                                                                                                                                                                                                                                                                                      |
| A07      | PASS stateless JWT chain and BCrypt strength verified in shared artifact; PASS secret no longer weak fallback; UNVERIFIED brute-force throttling, enumeration timing, refresh/logout revocation, re-authentication, and MFA (shared auth scope).                                                                                                                                                                                                                                                                                                                                                                        |
| A08      | PASS no Java native deserialization or unrestricted Jackson default typing found; PASS no client CDN scripts in backend; PASS Docker JWT secret now required; UNVERIFIED image signing, artifact promotion, CI SHA pinning, and secret scanning.                                                                                                                                                                                                                                                                                                                                                                        |
| A09      | PASS sensitive configuration redaction; PASS access failures remain server-visible through existing auth/service logging; UNVERIFIED centralized correlation IDs, durable audit trail, remote log storage, alert thresholds, and log-output scanning.                                                                                                                                                                                                                                                                                                                                                                   |
| A10      | PASS no exception body from User/Unit handlers; PASS file ingest cleans up stored files on failure; PASS request/upload caps; PASS security checks fail closed; UNVERIFIED one global RFC 9457 advice and exhaustive downstream failure/transaction probes across external SSH publishing.                                                                                                                                                                                                                                                                                                                              |

## Accepted risks and deferred items

| Item                                                                       | Category | Why deferred                                                                       | Proposed owner/date                          |
|----------------------------------------------------------------------------|----------|------------------------------------------------------------------------------------|----------------------------------------------|
| MFA, brute-force throttling, token revocation/rotation                     | A07      | Implemented in shared auth boundary, explicitly out of scope here                  | Shared auth owner / next auth release        |
| SCA/dependency-check, CycloneDX SBOM, image digest/signing, CI SHA pinning | A03/A08  | Existing backend has no authorized scanner/SBOM tooling; no tooling added silently | Platform/CI owner / next pipeline hardening  |
| Production TLS, DB `sslmode=verify-full`, SSH host-key policy              | A04/A08  | Deployment secrets/certificates and remote site are external to this repository    | Operations owner / before production rollout |
| Central RFC 9457 advice, correlation IDs, alerting/remote audit sink       | A09/A10  | Existing shared/runtime exception model spans shared auth and other services       | Backend platform owner / next API hardening  |

## Tests run

* `./gradlew :service:test --tests ...` targeted security/controller tests — passed.
* `./gradlew clean test` — passed: 721 tests completed, 3 skipped.

## Recommendations not implemented

1. Add dependency scanning and CycloneDX SBOM generation to the existing CI workflow after approval.
2. Pin the reusable GitHub Actions workflow by commit SHA and reduce top-level `contents/packages/id-token` permissions.
3. Move test JWT configuration to per-run generated material.
4. Add shared-auth MFA, account throttling, refresh-token rotation/revocation, and security-event alerting.
5. Run local dynamic probes in a production-profile deployment for anonymous, non-admin, admin, evil-origin, actuator, and
   both datasource scopes; no external services were contacted during this audit.

## Residual integration risks

File-backend ingestion/publishing, Website runtime authorization, shared-auth JWT lifecycle, remote SSH/SFTP host-key
verification, server-side page HTML sanitization, and CI dependency/SBOM scanning remain outside these repository changes
and must be addressed before a production security sign-off. Frontend injection sinks and reusable-workflow pinning are
covered by the companion frontend report.
