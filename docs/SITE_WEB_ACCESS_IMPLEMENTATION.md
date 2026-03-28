# Site Web Access Management Implementation

This document summarizes the implementation of the site web access management subsystem for Vempain Admin.

## Overview

The site web access management subsystem allows admin users to create and manage site web users (separate from admin users) and control their access to site
resources (files, galleries, pages) through ACL assignments.

## Architecture

### Database Schema (Already Existed)

- `web_site_users`: Stores site web user accounts with bcrypt-hashed passwords
- `web_site_acl`: Links site web users to ACL IDs (one-to-many relationship)
- Resources (`web_site_file`, `web_site_gallery`, `web_site_page`) have `acl_id` columns

### Components Created

#### 1. DTOs (api module)

**Request DTOs** (`api/src/main/java/fi/poltsi/vempain/admin/api/site/request/`):

- `WebSiteUserRequest`: Username and password for create/update operations
- `WebSiteAclRequest`: Links a user ID to an ACL ID

**Response DTOs** (`api/src/main/java/fi/poltsi/vempain/admin/api/site/response/`):

- `WebSiteUserResponse`: User info (no password hash exposed)
- `WebSiteAclResponse`: ACL assignment details
- `WebSiteUserResourcesResponse`: Lists resources accessible to a user
- `WebSiteAclUsersResponse`: Lists users assigned to an ACL

#### 2. Entities (service module)

Updated entities (`service/src/main/java/fi/poltsi/vempain/site/entity/`):

- `WebSiteUser`: Added `toResponse()` method
- `WebSiteAcl`: Added missing `aclId` field and `toResponse()` method

#### 3. Repositories (service module)

Created repositories (`service/src/main/java/fi/poltsi/vempain/site/repository/`):

- `WebSiteUserRepository`: Find by username, check existence
- `WebSiteAclRepository`: Find by ACL ID, user ID, custom queries for lookups

#### 4. Configuration (service module)

- `PasswordEncoderConfiguration`: Provides BCrypt password encoder bean (strength 12)

#### 5. Services (service module)

Created services (`service/src/main/java/fi/poltsi/vempain/site/service/`):

**WebSiteUserService**:

- `findAll()`: List all users
- `findById(userId)`: Get specific user
- `findByUsername(username)`: Get user by username
- `create(request)`: Create new user with password hashing
- `update(userId, request)`: Update username/password
- `delete(userId)`: Delete user (cascades to ACL entries)
- `changePassword(userId, password)`: Change password only

**WebSiteAclService**:

- `findAll()`: List all ACL entries
- `findById(id)`: Get specific ACL entry
- `findUsersByAclId(aclId)`: Get users for an ACL
- `findResourcesByUserId(userId)`: Get resources accessible to a user
- `create(request)`: Create ACL assignment
- `delete(id)`: Delete ACL entry
- `deleteByAclId(aclId)`: Delete all entries for an ACL

Both services use `AccessService.getValidUserId()` to track admin creator/modifier IDs.

#### 6. REST API (api module)

**SiteWebAccessAPI** (`api/src/main/java/fi/poltsi/vempain/admin/rest/`):

Endpoints under `/admin-management/site-access`:

**User Management**:

- `GET /users` - List all users
- `GET /users/{userId}` - Get user by ID
- `POST /users` - Create user
- `PUT /users/{userId}` - Update user
- `DELETE /users/{userId}` - Delete user

**ACL Management**:

- `GET /acls` - List all ACL entries
- `GET /acls/{aclId}/users` - Get users for ACL
- `GET /users/{userId}/resources` - Get resources for user
- `POST /acls` - Create ACL entry
- `DELETE /acls/{id}` - Delete ACL entry

All endpoints require Bearer authentication and are documented with Swagger annotations.

#### 7. Controller (service module)

**SiteWebAccessController** (`service/src/main/java/fi/poltsi/vempain/admin/controller/`):

- Thin wrapper around services
- Calls `accessService.checkAuthentication()` on all endpoints
- Returns `ResponseEntity` wrapping service responses
- Uses `@Valid` for request validation

## Security Features

1. **Password Hashing**: All passwords hashed with BCrypt (strength 12)
2. **Admin Authentication**: All endpoints require authenticated admin user
3. **Audit Trail**: Creator/modifier tracking using admin user IDs
4. **No Password Exposure**: Password hashes never returned in responses
5. **Cascade Delete**: Deleting a user automatically removes all ACL entries

## Key Design Decisions

1. **Separate User Types**: Site web users are completely separate from admin users
2. **ACL ID Model**: Resources have ACL IDs; web_site_acl links users to those IDs
3. **One-to-Many**: Each ACL ID can have multiple users
4. **Service Ownership**: Services handle all business logic; controllers are thin
5. **Validation**: Jakarta Bean Validation on request DTOs
6. **Transaction Management**: All write operations are `@Transactional`

## Testing Status

- Code compiles successfully
- No compilation errors
- Integration/unit tests not yet created (to be added)

## Next Steps

1. Add integration tests for repositories
2. Add unit tests for services
3. Add controller slice tests
4. Add end-to-end API tests
5. Consider adding batch operations (e.g., assign multiple users to ACL)
6. Add password strength validation rules
7. Consider adding "last login" tracking for web users

