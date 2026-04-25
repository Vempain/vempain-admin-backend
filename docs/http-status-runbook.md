# HTTP status runbook: 404 was surfacing as 403

## Why it happened
- Some admin endpoints signal missing resources by throwing `ResponseStatusException(HttpStatus.NOT_FOUND, ...)` from services/controllers.
- Those exception-based 404s go through servlet error dispatch (`/error`) instead of returning a direct `ResponseEntity.notFound()`.
- The admin backend was using the shared security chain without an explicit JWT `AuthenticationEntryPoint`, and `/error` was still secured, so the redispatch could be translated into `403 Forbidden`.

## What changed
- Added a local high-priority security chain in `service/src/main/java/fi/poltsi/vempain/AdminSecurityConfig.java`.
- `DispatcherType.ERROR` and `/error` are now permitted so MVC/Boot can render the original status.
- JWT auth failures now use `AuthEntryPointJwt`, so missing authentication returns `401` while real access denials still return `403`.

## Quick verification
```bash
TOKEN="<valid admin jwt>"

curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/content-management/data/does_not_exist
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/content-management/forms/999999
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/content-management/pages/999999
curl -i http://localhost:8080/api/content-management/data
```

Expected results:
- missing data set -> `404 Not Found`
- missing form -> `404 Not Found`
- direct page not found path -> `404 Not Found`
- no token -> `401 Unauthorized`

