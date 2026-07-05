---
title: API Design Guidelines
---

## Overview

This guide describes how we design HTTP JSON APIs for Worxbend services written on the JVM. It is written for Scala and Java engineers who build routes, DTOs, domain services, persistence adapters, and client SDKs. The goal is a stable API surface that is easy to document, generate clients for, test, monitor, and evolve.

The reference material behind this guide includes REST, HTTP semantics, and resource-oriented API guidance. We use those sources as design input, not as text or examples to copy. The examples here use JVM-service domains such as workspaces, projects, build runs, artifacts, deployments, and jobs.

Use this default rule: expose user-visible resources, operate on them with standard HTTP methods, make every request self-contained, and keep implementation details behind DTO and domain boundaries.

## One-Page Rules

Start here when designing or reviewing an endpoint.

| Area             | Good default                                                                    | Avoid                                                            |
| ---------------- | ------------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| Resource model   | Model nouns that users recognize: projects, builds, artifacts, deployments.     | Modeling controllers, tables, jobs, or service classes directly. |
| URL shape        | `/v1/workspaces/{workspaceId}/projects/{projectId}`.                            | `/v1/projectService/getProject` or `/v1/projects/get`.           |
| Create           | `POST /v1/workspaces/{workspaceId}/projects` with a JSON body.                  | `POST /v1/projects/create`.                                      |
| Read one         | `GET /v1/workspaces/{workspaceId}/projects/{projectId}`.                        | `POST /v1/projects/get`.                                         |
| Read many        | `GET /v1/workspaces/{workspaceId}/projects?pageSize=50&pageToken=...`.          | Returning an unbounded collection.                               |
| Update           | `PATCH /v1/workspaces/{workspaceId}/projects/{projectId}` with changed fields.  | `PUT` by habit or a custom `updateName` endpoint.                |
| Delete           | `DELETE /v1/workspaces/{workspaceId}/projects/{projectId}`.                     | Delete endpoints with complex JSON bodies.                       |
| Custom action    | `POST /v1/projects/{projectId}/build-runs/{runId}:cancel`.                      | Custom actions that duplicate create, update, or delete.         |
| Errors           | Stable status code, stable error code, human message, optional details.         | Parsing English text or leaking stack traces.                    |
| Idempotency      | Use idempotency keys for retry-sensitive mutating calls.                        | Letting client retries create duplicate resources.               |
| Compatibility    | Add optional fields and endpoints.                                              | Rename fields, change meanings, or add required request fields.  |
| JVM boundary     | Convert JSON DTOs to domain commands before running business logic.             | Letting wire DTOs become the domain model by accident.           |

## Design Mindset

A good API is a product contract, not a database dump and not a remote method table. The URL, method, request body, response body, status code, headers, and documentation together form one interface. JVM implementation details such as Tapir endpoints, Spring controllers, repository classes, SQL schemas, queues, and workers should not shape the public contract directly.

REST contributes the most important constraints: identify resources, use standard method semantics, keep requests stateless, return representations, and make caching and intermediaries possible where appropriate. Resource-oriented API guidance contributes a practical discipline: design resources first, apply standard methods consistently, and use custom actions sparingly when lifecycle methods do not express the operation.

For Scala and Java services, this means we should design in this order:

1. Define the user-visible resources.
2. Define ownership and hierarchy.
3. Define JSON DTOs and validation rules.
4. Define standard operations.
5. Add custom actions only after standard operations fail to fit.
6. Define errors, idempotency, authorization, pagination, and compatibility rules before implementation.

## Resource Modeling

Resource modeling is the highest-leverage part of API design. If resources are wrong, route names, permissions, persistence, events, and clients all become harder.

### Choose Resources

Start with the concepts a caller manages. Use the database schema only as supporting evidence.

| Question                                    | Example answer          | API shape                                                     |
| ------------------------------------------- | ----------------------- | ------------------------------------------------------------- |
| What does the caller manage?                | Workspace, project      | `/v1/workspaces`, `/v1/workspaces/{workspaceId}/projects`     |
| What is created repeatedly under a parent?  | Build runs              | `/v1/projects/{projectId}/build-runs`                         |
| What is downloaded or inspected later?      | Artifacts               | `/v1/build-runs/{runId}/artifacts/{artifactId}`               |
| What represents an environment state?       | Deployment              | `/v1/environments/{environmentId}/deployments/{deploymentId}` |
| What is just an attribute?                  | Project display name    | Field on `Project`, not `/projects/{id}/display-name`.        |
| What relationship needs its own metadata?   | Project membership role | `/v1/projects/{projectId}/memberships/{membershipId}`         |

Do not mirror tables. A `project_members` join table might become a `Membership` resource because callers manage roles and audit changes. A table column such as `last_seen_at` usually remains a field.

### Choose Ownership

Every resource should have one canonical parent. A canonical parent defines route shape, permission checks, lifecycle, and audit context.

Use this resource tree for a build service:

```text
workspaces/{workspaceId}
workspaces/{workspaceId}/projects/{projectId}
projects/{projectId}/build-runs/{runId}
build-runs/{runId}/artifacts/{artifactId}
environments/{environmentId}/deployments/{deploymentId}
```

Rules:

- Use one canonical path for a resource.
- Use links or fields for secondary relationships.
- Do not put the same resource under multiple parent paths unless one path is a read-only alias with clear semantics.
- Keep parent-child relationships acyclic.
- Use flat paths when a deeply nested path adds no authorization or lifecycle value.

### Name Paths

Use plural collection names and stable path parameters.

| Resource      | Path segment       | Parameter         |
| ------------- | ------------------ | ----------------- |
| Workspace     | `workspaces`       | `{workspaceId}`   |
| Project       | `projects`         | `{projectId}`     |
| Build run     | `build-runs`       | `{runId}`         |
| Artifact      | `artifacts`        | `{artifactId}`    |
| Deployment    | `deployments`      | `{deploymentId}`  |
| Membership    | `memberships`      | `{membershipId}`  |

Use kebab-case for URL path segments. Use lower camel case for JSON fields if the Java and Scala stack serializes that shape by default. Pick one JSON field style per service family and keep it stable.

Good path examples:

```http
GET /v1/workspaces/ws-123/projects/prj-456
GET /v1/projects/prj-456/build-runs/run-789
GET /v1/build-runs/run-789/artifacts/artifact-logs
```

Bad path examples:

```http
GET /v1/project/get?id=prj-456
POST /v1/buildRunService/startBuildRun
GET /v1/db/project_rows/456
```

### Design DTO Fields

Fields should make ownership, identity, timestamps, concurrency, and lifecycle visible.

| Field              | Use                                                                 |
| ------------------ | ------------------------------------------------------------------- |
| `id`               | Local resource identifier within its collection.                    |
| `uri`              | Canonical API path when clients benefit from storing it.            |
| `displayName`      | Human-readable name.                                                |
| `createdAt`        | Server-set creation timestamp.                                      |
| `updatedAt`        | Server-set last update timestamp.                                   |
| `deletedAt`        | Server-set soft delete timestamp.                                   |
| `version`          | Optimistic concurrency token when `ETag` is not enough.             |
| `etag`             | HTTP concurrency token for conditional update or delete.            |
| `state`            | Output lifecycle state such as `QUEUED`, `RUNNING`, `SUCCEEDED`.   |
| `idempotencyKey`   | Client-supplied retry key for mutating requests.                    |
| `pageSize`         | Maximum number of items requested by a list call.                   |
| `pageToken`        | Opaque token for list continuation.                                 |
| `nextPageToken`    | Opaque token returned when another page exists.                     |
| `filter`           | Documented filter expression.                                       |
| `orderBy`          | Documented sort expression.                                         |

Separate request DTOs from response DTOs when fields differ. A `CreateProjectRequest` should not accept `createdAt`, `updatedAt`, `state`, `etag`, or server-owned IDs unless the API explicitly supports caller-chosen IDs.

## Standard Methods

Most resources need the same small set of operations. Standard operations are boring by design; they make client code predictable.

| Operation | HTTP shape                                             | Body              | Success response                             |
| --------- | ------------------------------------------------------ | ----------------- | -------------------------------------------- |
| Get       | `GET /v1/workspaces/{workspaceId}/projects/{projectId}` | None              | `200 OK` with a `ProjectResponse`.           |
| List      | `GET /v1/workspaces/{workspaceId}/projects`             | None              | `200 OK` with items and `nextPageToken`.     |
| Create    | `POST /v1/workspaces/{workspaceId}/projects`            | Create request    | `201 Created` with a `ProjectResponse`.      |
| Update    | `PATCH /v1/workspaces/{workspaceId}/projects/{projectId}` | Patch request   | `200 OK` with the updated `ProjectResponse`. |
| Delete    | `DELETE /v1/workspaces/{workspaceId}/projects/{projectId}` | None           | `204 No Content` or `200 OK` with status.    |

### Get

Use Get for one resource by canonical path:

```http
GET /v1/workspaces/ws-123/projects/prj-456
Accept: application/json
```

Return the representation the client can use immediately:

```json
{
  "id": "prj-456",
  "uri": "/v1/workspaces/ws-123/projects/prj-456",
  "displayName": "Billing API",
  "state": "ACTIVE",
  "createdAt": "2026-07-05T10:30:00Z",
  "updatedAt": "2026-07-05T10:30:00Z",
  "etag": "\"project-7\""
}
```

Rules:

- Do not use a request body.
- Return `404 Not Found` when the resource is absent and the caller may know that.
- Return `403 Forbidden` when the caller lacks access.
- Use `ETag` and `Cache-Control` when the resource is safe to cache.
- Avoid optional response fields whose absence changes the meaning of the resource.

### List

Use List for a finite, paginated collection:

```http
GET /v1/workspaces/ws-123/projects?pageSize=50&pageToken=opaque-token&filter=state%20%3D%20ACTIVE&orderBy=displayName
Accept: application/json
```

Return items and an opaque continuation token:

```json
{
  "items": [
    {
      "id": "prj-456",
      "uri": "/v1/workspaces/ws-123/projects/prj-456",
      "displayName": "Billing API",
      "state": "ACTIVE"
    }
  ],
  "nextPageToken": "next-opaque-token"
}
```

Rules:

- Add pagination from the first release of every collection endpoint.
- Treat `pageToken` as opaque; clients must never parse it.
- Let the server choose a default `pageSize`.
- Enforce a documented maximum `pageSize`.
- Keep all query parameters the same when using `nextPageToken`, except `pageSize` if the service supports changing it.
- Document default ordering.
- Add filtering only for fields that have clear indexes or performance bounds.

### Create

Use Create to add a resource to a collection:

```http
POST /v1/workspaces/ws-123/projects
Content-Type: application/json
Accept: application/json
Idempotency-Key: 7d9b3c1c-42db-44ef-8f64-e4ad9baf8221

{
  "id": "billing-api",
  "displayName": "Billing API",
  "repositoryUri": "https://github.com/worxbend/billing-api"
}
```

Return the created resource:

```json
{
  "id": "billing-api",
  "uri": "/v1/workspaces/ws-123/projects/billing-api",
  "displayName": "Billing API",
  "repositoryUri": "https://github.com/worxbend/billing-api",
  "state": "ACTIVE",
  "createdAt": "2026-07-05T10:30:00Z",
  "updatedAt": "2026-07-05T10:30:00Z",
  "etag": "\"project-1\""
}
```

Rules:

- Use `POST` on the parent collection.
- Return `201 Created` when the resource is created synchronously.
- Include a `Location` header with the canonical URI.
- Support an idempotency key when retries could create duplicates.
- Validate IDs at the boundary and return a structured validation error.
- If creation starts long-running work, return `202 Accepted` with an operation resource or job resource.

### Update

Use `PATCH` for partial updates:

```http
PATCH /v1/workspaces/ws-123/projects/billing-api
Content-Type: application/json
Accept: application/json
If-Match: "project-1"

{
  "displayName": "Billing Platform API",
  "repositoryUri": "https://github.com/worxbend/billing-platform"
}
```

Return the updated resource:

```json
{
  "id": "billing-api",
  "uri": "/v1/workspaces/ws-123/projects/billing-api",
  "displayName": "Billing Platform API",
  "repositoryUri": "https://github.com/worxbend/billing-platform",
  "state": "ACTIVE",
  "updatedAt": "2026-07-05T11:00:00Z",
  "etag": "\"project-2\""
}
```

Rules:

- Prefer `PATCH` for API evolution because clients can send only intended changes.
- Use `If-Match` or an explicit version when lost updates matter.
- Reject immutable field changes with a clear validation error.
- Treat omitted fields as unchanged.
- Treat explicit `null` carefully; prefer dedicated clear operations or nullable fields only where absence is a real domain concept.
- Use `PUT` only when full replacement semantics are truly required and documented.

### Delete

Use Delete for one resource:

```http
DELETE /v1/workspaces/ws-123/projects/billing-api
Accept: application/json
If-Match: "project-2"
```

Rules:

- Do not require a request body.
- Return `204 No Content` for successful hard deletion.
- Return `200 OK` with a final representation when soft deletion is visible to clients.
- Return `409 Conflict` or `412 Precondition Failed` when state or concurrency prevents deletion.
- Do not cascade by surprise. If children are deleted, document the cascade and consider a `force=true` query parameter.

## Custom Actions

Custom actions are for workflow operations that do not naturally fit create, read, update, or delete. They are useful for APIs such as build systems, deployment platforms, schedulers, and document processors, but they should stay rare.

Use custom actions for these cases:

| User intent             | HTTP shape                                             | Why it is not standard CRUD                  |
| ----------------------- | ------------------------------------------------------ | -------------------------------------------- |
| Cancel a running build. | `POST /v1/projects/prj-456/build-runs/run-789:cancel`  | State transition with operational semantics. |
| Retry a failed job.     | `POST /v1/jobs/job-123:retry`                          | Creates a controlled retry attempt.          |
| Promote a deployment.   | `POST /v1/environments/prod/deployments/dep-7:promote` | Domain workflow, not a field patch.          |
| Validate a config.      | `POST /v1/projects/prj-456/config:validate`            | No durable resource is created.              |
| Export artifacts.       | `POST /v1/build-runs/run-789/artifacts:export`         | Starts work that may outlive the request.    |

Rules:

- Prefer standard operations first.
- Use `POST` for mutating or workflow actions.
- Use `GET` only for custom reads with no side effects and modest query parameters.
- Put the action after a colon at the end of the resource or collection path.
- Name the action as a verb: `:cancel`, `:retry`, `:promote`, `:validate`, `:export`.
- Avoid action names that duplicate CRUD: `:create`, `:update`, `:delete`, `:get`, `:list`.
- Keep actions stateless from the protocol perspective: every request contains the data needed to process it.
- Return the affected resource, an operation resource, or a clear action result DTO.

Bad custom actions:

```http
POST /v1/projects/get
POST /v1/projects/updateDisplayName
POST /v1/build-runs/delete
POST /v1/jobs/retryFailedJobsForProjectWithReason
```

Better alternatives:

```http
GET    /v1/workspaces/ws-123/projects/prj-456
PATCH  /v1/workspaces/ws-123/projects/prj-456
DELETE /v1/projects/prj-456/build-runs/run-789
POST   /v1/projects/prj-456/jobs:retry
```

## JVM Implementation Guidance

The public API and the JVM implementation should be aligned, but not identical. A good Scala or Java service keeps transport DTOs, domain commands, services, repositories, and external adapters separate enough that one layer can evolve without dragging the others.

### DTOs And Domain Models

Use request and response DTOs at the HTTP boundary. Convert DTOs into domain commands before business logic.

Recommended flow:

```text
HTTP request
  -> route/controller DTO validation
  -> domain command
  -> service method
  -> repository/client adapters
  -> domain result
  -> response DTO
  -> HTTP response
```

Rules:

- DTOs describe the wire contract.
- Domain types describe business invariants.
- Do not put persistence annotations on public response DTOs.
- Do not expose ORM entities, database rows, queue messages, or generated client models as API responses.
- Keep DTO field names stable even when internal domain names improve.
- Use typed IDs in the domain, even if the wire format is a string.

### Validation

Validate in layers:

| Layer              | Validates                                                                 |
| ------------------ | ------------------------------------------------------------------------- |
| HTTP boundary      | JSON shape, required fields, basic formats, path/body consistency.        |
| Domain constructor | Invariants such as non-empty names, valid state transitions, permissions. |
| Persistence layer  | Uniqueness, foreign key existence, transaction constraints.               |
| External adapter   | Upstream-specific constraints and error mapping.                          |

Return all useful field validation errors at once when possible. Do not make clients fix one missing field per request round trip.

### Errors

Use a stable error envelope. For JVM services, this maps well from Scala ADTs or Java sealed interfaces into HTTP responses.

Use this shape unless a service already has a stronger standard:

```json
{
  "error": {
    "code": "PROJECT_NOT_FOUND",
    "message": "Project 'prj-456' was not found.",
    "status": 404,
    "details": {
      "projectId": "prj-456"
    }
  }
}
```

Rules:

- `code` is stable and machine-readable.
- `message` is short and safe for humans.
- `status` matches the HTTP status code.
- `details` contains structured values clients may use.
- Do not expose class names, SQL messages, stack traces, hostnames, or internal service names.
- Map recoverable domain failures explicitly.
- Let unexpected defects become `500 Internal Server Error` after logging with correlation data.

### Status Codes

Use a small, consistent subset.

| Status                  | Use when                                                     |
| ----------------------- | ------------------------------------------------------------ |
| `200 OK`                | Successful read, update, or action with a response body.     |
| `201 Created`           | Synchronous create produced a resource.                      |
| `202 Accepted`          | Work started and continues asynchronously.                   |
| `204 No Content`        | Delete or action succeeded with no body.                     |
| `400 Bad Request`       | Request syntax, validation, or field shape is invalid.       |
| `401 Unauthorized`      | Authentication is missing or invalid.                        |
| `403 Forbidden`         | Caller is authenticated but lacks permission.                |
| `404 Not Found`         | Resource is absent or intentionally hidden from the caller.  |
| `409 Conflict`          | Request conflicts with current resource state.               |
| `412 Precondition Failed` | Concurrency precondition such as `If-Match` failed.        |
| `429 Too Many Requests` | Caller exceeds quota or rate limits.                         |
| `500 Internal Server Error` | Unexpected server failure.                               |
| `503 Service Unavailable` | Temporary overload or dependency outage.                   |

## Pagination, Filtering, And Sorting

Collections grow. A list endpoint without pagination is a future outage or a future breaking change.

### Pagination

Every list endpoint returns a bounded page:

```http
GET /v1/projects/prj-456/build-runs?pageSize=100&pageToken=opaque-token
```

The response carries the next token:

```json
{
  "items": [
    {
      "id": "run-789",
      "uri": "/v1/projects/prj-456/build-runs/run-789",
      "state": "SUCCEEDED"
    }
  ],
  "nextPageToken": "next-opaque-token"
}
```

Rules:

- `pageToken` is opaque.
- An empty or missing `nextPageToken` means the collection is complete.
- The server may return fewer items than requested.
- The server enforces maximum page size.
- The default ordering must be stable while paging.

### Filtering

Use one documented `filter` query parameter when filtering is needed:

```http
GET /v1/projects/prj-456/build-runs?filter=state%20%3D%20FAILED%20AND%20createdAt%20%3E%3D%20%222026-07-01T00%3A00%3A00Z%22
```

Rules:

- Document supported fields and operators.
- Reject unsupported filters with `400 Bad Request`.
- Keep the grammar small.
- Do not expose database column names.
- Do not support expensive filters unless the service has indexes or limits to protect itself.

### Sorting

Use one documented `orderBy` query parameter:

```http
GET /v1/projects/prj-456/build-runs?orderBy=createdAt desc
```

Rules:

- Document the default order.
- Sort only by stable, documented fields.
- Ensure sorting works with pagination.
- Avoid sorting by fields that change frequently during paging.

## Idempotency And Retries

Network calls fail in ambiguous places. The server might create the resource and the client might still see a timeout. Design mutating operations so clients can retry safely.

Rules:

- Use an `Idempotency-Key` header or `idempotencyKey` field for retry-sensitive creates and actions.
- Store the key with enough request identity to detect mismatched retries.
- Return the original successful response for a duplicate key within the retention window.
- Make `GET`, `PUT`, and `DELETE` idempotent according to HTTP semantics.
- Use `If-Match` and `ETag` or explicit versions for optimistic concurrency.
- Return `Retry-After` for rate limiting and temporary overload when the client should back off.

## Long-Running Work

Do not hold normal HTTP requests open for work that may outlive the request timeout. Build systems, exports, imports, migrations, deployments, and report generation usually need an explicit operation or job resource.

Start work with `202 Accepted`:

```http
POST /v1/projects/prj-456/build-runs
Content-Type: application/json
Accept: application/json
Idempotency-Key: 9d9c20e5-1f0e-4fb2-91a0-00b0ff47fd3e

{
  "gitRef": "main",
  "pipeline": "release"
}
```

Return a job-like resource:

```json
{
  "id": "run-789",
  "uri": "/v1/projects/prj-456/build-runs/run-789",
  "state": "QUEUED",
  "createdAt": "2026-07-05T10:30:00Z"
}
```

Clients poll the resource:

```http
GET /v1/projects/prj-456/build-runs/run-789
```

Rules:

- The returned resource has a stable URI.
- The resource exposes state and timestamps.
- Terminal states are explicit: `SUCCEEDED`, `FAILED`, `CANCELLED`.
- Failure details are structured and safe.
- Cancellation is a custom action when supported.
- Completed work may expire only if the retention policy is documented.

## Versioning And Compatibility

Versioning is a promise about client stability. Most changes should not require a new major version.

Rules:

- Put the major API version in the path: `/v1`, `/v2`.
- Avoid minor versions in paths such as `/v1.1`.
- Add optional request fields, response fields, endpoints, and enum values carefully.
- Do not add new required fields to existing request DTOs.
- Do not remove, rename, or retype existing fields.
- Do not change the meaning of an existing field or status code.
- Do not change default ordering in a way that breaks paginated clients.
- Use a new major version for breaking changes.
- Support old and new versions in parallel during migration.

For JVM code, preserve wire compatibility even if domain code changes. A Scala case class rename or Java record rename is not a valid reason to rename a JSON field.

## Security And Privacy

Security rules belong in the API contract and the implementation.

Rules:

- Use HTTPS only.
- Authenticate every non-public request.
- Authorize by resource, action, and caller.
- Validate path IDs against the authenticated tenant or workspace.
- Do not trust tenant, workspace, or user IDs just because they appear in a JWT or request body.
- Do not put secrets in URLs.
- Redact secrets from logs, traces, metrics, errors, and audit events.
- Make destructive and privilege-changing operations auditable.
- Prefer least-privilege scopes and permissions.

## Endpoint Documentation Template

Every endpoint should document the same facts so clients know what to send, what they receive, and how errors behave.

Use this template:

```markdown
### Create Project

Creates a project inside a workspace.

`POST /v1/workspaces/{workspaceId}/projects`

Request:

- `workspaceId` is required in the path.
- Body is `CreateProjectRequest`.
- `Idempotency-Key` is recommended for client retries.

Response:

- `201 Created` with `ProjectResponse`.
- `Location` contains the canonical project URI.

Errors:

- `400 PROJECT_ID_INVALID` when the project ID is malformed.
- `401 UNAUTHENTICATED` when credentials are missing or invalid.
- `403 WORKSPACE_FORBIDDEN` when the caller cannot create projects in the workspace.
- `404 WORKSPACE_NOT_FOUND` when the workspace is absent or hidden from the caller.
- `409 PROJECT_ALREADY_EXISTS` when the ID is already used.
```

## Design Checklist

Use this checklist before implementing a new endpoint.

### Resource Model

- The endpoint exposes a user-visible resource or a justified custom action.
- The resource has one canonical path.
- Collection names are plural nouns.
- Path parameters use stable names such as `{projectId}` and `{runId}`.
- Secondary relationships are fields or links, not duplicate canonical paths.
- The API does not expose table names, queue names, class names, or package names.

### HTTP Semantics

- `GET` and `DELETE` do not require request bodies.
- `POST` creates resources or starts actions.
- `PATCH` changes only supplied fields.
- Status codes are predictable and documented.
- Headers such as `Location`, `ETag`, `If-Match`, `Cache-Control`, and `Retry-After` are used deliberately.

### JVM Boundary

- Request DTOs are separate from domain commands when invariants differ.
- Response DTOs do not expose persistence entities.
- Domain errors map explicitly to HTTP errors.
- Unexpected exceptions are logged with correlation context and returned as safe `500` errors.
- Validation happens at both the wire boundary and the domain boundary.

### Collections

- List endpoints are paginated from the first release.
- Page tokens are opaque.
- Default ordering is documented and stable.
- Filtering and sorting are documented and bounded.

### Compatibility

- No new required request fields are added to existing endpoints.
- Existing field names, types, meanings, and status semantics remain stable.
- New enum values are safe for older clients.
- Breaking changes go into a new major version.
- Deprecations include replacement guidance and a removal window.

## References

These references shaped the guide, but the examples and structure above are adapted for Worxbend JVM HTTP APIs:

- [Google AIP-121 Resource-oriented design](https://google.aip.dev/121)
- [Google AIP-136 Custom methods](https://google.aip.dev/136)
- [Google AIP-158 Pagination](https://google.aip.dev/158)
- [Google AIP-160 Filtering](https://google.aip.dev/160)
- [Google AIP-161 Field masks](https://google.aip.dev/161)
- [Google AIP-180 Backwards compatibility](https://google.aip.dev/180)
- [Google AIP-185 API Versioning](https://google.aip.dev/185)
- [Google AIP-193 Errors](https://google.aip.dev/193)
- [RFC 9110 HTTP Semantics](https://datatracker.ietf.org/doc/html/rfc9110)
- [RFC 5789 PATCH Method for HTTP](https://datatracker.ietf.org/doc/html/rfc5789)
- [Roy Fielding: REST APIs must be hypertext-driven](https://roy.gbiv.com/untangled/2008/rest-apis-must-be-hypertext-driven)
