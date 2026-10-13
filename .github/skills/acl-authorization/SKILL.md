---
name: acl-authorization
description: Implement authorization in the admin backend the Vempain way (resource ACLs through AccessService, administrator ACL for admin endpoints, fail closed, granted and denied tests). Use whenever a change reads, lists, mutates or publishes a resource.
---

# Authorization in vempain-admin-backend

- Resource based, never role based: no `hasRole`, `ROLE_*`, `@PreAuthorize("hasRole(...)")` or test-mode bypass (`AccessService` has none).
- ACL-linked resources extend `AbstractVempainEntity`: `Component`, `Form`, `Layout`, `Page`, `Gallery`, `SiteFile` (plus `UserAccount`/`Unit`
  from `vempain-auth`). Check them with `accessService.hasRead/Modify/Create/DeletePermission(aclId)`, list them with
  `accessService.readableSpecification()`.
- Entities without an ACL (`DataEntity`, `Subject`, `Language`, `PublishSchedule`, `ScanQueueSchedule`, `FileThumb`, `GpsLocation`, the
  `site.*` tables, tags) are never ACL checked; their administration endpoints use `accessService.checkAdminAccess()` = modify privilege on
  the administrator ACL (`Constants.ADMIN_ID`).
- Publishing is authorized as a whole (`PublishService.authorizePagePublish`/`authorizeGalleryPublish`): modify on the page, read on form,
  layout, components, modify on each gallery and read on each of its files; bulk variants skip what the user may not publish.
- Fail closed: a non-positive `acl_id` or missing rows deny; `AclConsistencySchedule` repairs such rows. Units nest and membership is
  transitive (`UserDetailsServiceImpl` expands units), so grant tests through units when relevant.
- Service-to-service callers (file backend) use an API token (`ApiTokenAuthenticationFilter`, `X-Vempain-Api-Token`) that acts as the
  creating administrator with the `API_TOKEN` authority and only on the endpoints in its `ALLOWED` list; add an endpoint there only when
  the file backend needs it.
- Tests: ITCs run as the seeded administrator; use `TestITCTools.generateAcl` (admin granted) and `generateAclForOwnerOnly(strangerId, ...)`
  plus `authenticateAs(userId)` for denied cases. Every ACL path has both cases (`AccessServiceITC/UTC`, `PublishServiceITC/UTC`).
- Record changes in `security/OWASP-2025-audit-report.md`.
