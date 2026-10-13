---
name: add-dual-datasource-migration
description: Add a Flyway migration or persisted field to the admin backend, which runs two PostgreSQL datasources (admin and site) with separate migration trees. Use for schema changes, new columns or data backfills.
---

# Migrations in vempain-admin-backend

- Two datasources, two Flyway trees (`FlywayMultiDBConfiguration`):
    - admin schema: `service/src/main/resources/db/migration/admin` (`AdminDatabaseConfiguration`, repositories under
      `fi.poltsi.vempain.admin.repository`, task tables `V1004__task_tables.sql`, API tokens `V1005__api_tokens.sql`);
    - site schema: `service/src/main/resources/db/migration/site` (`SiteDatabaseConfiguration`, `fi.poltsi.vempain.site.*`); the website
      backend reads this schema read-only, so a site change must keep `vempain-website-backend` entities and DTO contracts in step.
- The auth migrations (`db/migration/auth`) come from the `vempain-auth-core` jar and run on the admin Flyway: never reuse their version
  numbers in `db/migration/admin`.
- A persisted field means migration + entity + request/response DTO + service mapping (`toResponse()` on the entity when the pattern
  exists) + tests, in one change (reference: `global_permission`, `V1002__add_global_permission_on_user.sql`, `WebSiteUser`).
- Derived data is rebuilt in the migration, not by hand (reference: `V1006__page_gallery_from_embeds.sql`).
- `AbstractITCTest` runs Flyway clean+migrate before each test, so any ITC verifies the migration; add an ITC for repository behaviour and a
  UTC for the service mapping.
- Publishing CSV into the site datasource is schema driven; keep the ISO date/time coercion and the `400` on invalid input when touching
  `DataService`.
