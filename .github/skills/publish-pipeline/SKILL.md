---
name: publish-pipeline
description: Change how pages, galleries and files are published from the admin backend to the site (site datasource writes, SFTP transfer with JschClient, file ingest from the file backend). Use for publishing, ingest or site-schema related work.
---

# Publishing in vempain-admin-backend

- `PublishService.publishPageAsSystem` renders the page body from layout, form components and page body into `WebSitePage` (site
  datasource), then publishes every gallery linked through `page_gallery` (derived from the body's `gallery`/hero `carousel` embeds, sent by
  the admin frontend as `PageRequest.gallery_ids`; never edited by hand).
- `publishGalleryAsSystem` transfers the site files and thumbnails over SFTP (`fi.poltsi.vempain.tools.JschClient`, settings
  `vempain.site.ssh.*`, `vempain.admin.ssh.*`, `vempain.site.www-root`, `vempain.site.thumb-directory`) and then rewrites the site rows:
  `WebSiteGallery`, `WebSiteFile` (+ `WebGpsLocation`), gallery-file relations and subjects. The transfer calls back per file so the task
  can count steps; the whole gallery is one transaction.
- Incoming files from the file backend: `POST /content-management/file/site-file` (`FileIngestService`, API token authentication). It
  creates or updates the `SiteFile`, its thumbnail, the gallery link and the optional extra ACLs (`FileIngestRequest.acls`, validated
  before any write). `DELETE /content-management/file/site-file/{id}` is the undo used by cancelled file backend tasks.
- Data sets: `DataService` publishes CSV into `website_data__*` tables with schema-driven coercion; `DELETE /content-management/data/{identifier}`
  is the undo.
- Changing the site schema or the shape of published rows: update `vempain-website-backend` (read-only consumer) and its `ResponseContractJTC`
  plus `vempain-website-frontend` models in the same change set.
- `exiftool`, the SSH key and the directories must exist at startup (`SetupVerification`); `testSetup.sh --developer-name <login>` (root)
  creates the local users, keys and directories the ITCs need, `testCleanup.sh` removes them.
- Tests: `PublishServiceUTC` (mocked `JschClient`), `PublishServiceITC` (real datasources, SFTP mocked in `AbstractITCTest`),
  `JschClientUTC` (mocked `ChannelSftp`, transfer order), `FileIngestServiceUTC/ITC`, `FileIngestCTC`, `DataServiceITC`.
