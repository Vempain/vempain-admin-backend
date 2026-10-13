---
name: add-rest-endpoint
description: Add or change a REST endpoint of the Vempain admin backend (api/ contract, service/ controller, AccessService checks, tests, CLI sync). Use for new endpoints, DTOs, paged lists or request/response fields.
---

# Add a REST endpoint to vempain-admin-backend

## Where things go

1. Contract: `api/src/main/java/fi/poltsi/vempain/admin/rest/**/<Name>API.java`; path prefixes come from
   `api/.../api/Constants.java` (`/content-management`, `/admin-management`, `/schedule-management`). Swagger annotations on every method.
2. DTOs: `api/.../api/request|response/**`, Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`, `@JsonNaming(SnakeCaseStrategy)`
   from `tools.jackson.databind`, `@Schema` on class and fields, Jakarta validation. JSON is snake_case only. Types that the file backend
   also needs go to `vempain-common` (release it first); this API must never depend on `vempain-file-backend-api`.
3. Controller: `service/.../controller/**` implements the interface, calls `accessService.checkAuthentication()`, logs, delegates. Write
   paths take the audit user from `accessService.getValidUserId()`.
4. Service: `service/.../service/**` holds the rules and the authorization (see the `acl-authorization` skill); repositories in
   `service/.../repository/**` (admin datasource) or `fi.poltsi.vempain.site.repository` (site datasource, read/write only through publishing).
5. Keep request normalisation in the service (`sanitizePageable()` pattern), build every `LIKE` with `fi.poltsi.vempain.tools.LikePatterns`
   and an explicit escape, allow-list sort columns.

## Tests (mandatory)

- `*CTC` (MockMvc on the integration context, e.g. `LayoutCTC`, `ApiTokenCTC`) and `*ITC` (`AbstractITCTest`: two Postgres containers,
  Flyway clean+migrate per test, `authenticateAs(userId)`, `TestITCTools.generateAcl` / `generateAclForOwnerOnly`). Granted and denied case
  for every ACL dependent path.
- `*UTC` for the service with Mockito; `*JTC` in `api/` for shared contracts (`RestContractJTC`, `FileIngestAclContractJTC`).
- Run `./gradlew :service:test --tests '*<Name>*'` and report; the ITC environment needs `testSetup.sh` (see `run-backend-tests`).

## Finish

- Update `AGENTS.md`, `README.md`/`docs/` when behaviour is user facing, `security/OWASP-2025-audit-report.md` for authorization, query
  building, SSH or input handling changes.
- Review `vempain-cli` for every API change; the file backend consumes this API, so release this repository before the file backend.
