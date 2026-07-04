---
title: API Design Guidelines
---

## Overview

This page is a practical guide for designing HTTP JSON APIs that feel consistent, boring, and easy to consume. It combines the useful parts of canonical REST, HTTP semantics, and Google's API Improvement Proposals, especially [AIP-121 Resource-oriented design](https://google.aip.dev/121) and [AIP-136 Custom methods](https://google.aip.dev/136).

Use this as the default rule: model the API around resources, give every important thing a stable name, use standard HTTP methods for standard actions, and reserve custom actions for cases where the standard methods do not fit.

## Short Version

If we only remember one section, remember this one.

| Rule             | Good default                                                                 | Avoid                                                              |
| ---------------- | ---------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| Model            | Resources and collections.                                                   | Endpoint names that describe implementation functions.             |
| URLs             | `/v1/projects/{project}/books/{book}`.                                       | `/v1/getBook`, `/v1/bookService/delete`, `/v1/books/delete/{id}`.  |
| Methods          | `GET`, `POST`, `PATCH`, `DELETE`.                                            | Inventing HTTP verbs or using `POST` for every operation.          |
| Create           | `POST /v1/{parent}/books` with the resource in the body.                     | `POST /v1/books/create`.                                           |
| Read one         | `GET /v1/{name}`.                                                            | `POST /v1/books/get`.                                              |
| Read many        | `GET /v1/{parent}/books?page_size=50&page_token=...`.                        | Unbounded list responses.                                          |
| Update           | `PATCH /v1/{book.name}` with `update_mask`.                                  | Full replacement unless the API can truly support it forever.      |
| Delete           | `DELETE /v1/{name}` with no body.                                            | DELETE requests with complex JSON bodies.                          |
| Custom action    | `POST /v1/{name}:archive` only when the action is not create/update/delete.  | Custom verbs that duplicate standard methods.                      |
| Errors           | Stable status, actionable message, machine-readable details.                 | Messages that expose internals or require string parsing.          |
| Versioning       | Major version only: `/v1`, `/v1beta`, `/v2`.                                 | `/v1.1`, `/v1.2.3`, date versions for every release.               |
| Compatibility    | Add optional fields and new resources.                                       | Removing fields, renaming fields, or adding new required fields.   |

## Mental Model

REST is an architectural style, not just "JSON over HTTP." Strict REST emphasizes a uniform interface, stateless requests, cacheable responses, layered systems, and hypermedia-driven state transitions. Roy Fielding's reminder is important: a true REST API is driven by representations and links, not by clients hard-coding every URI and workflow from external documentation.

Google's AIPs are more pragmatic. They define resource-oriented APIs that look like RESTful HTTP APIs and are also easy to express as RPCs. That means we can use stable resource paths and predictable method patterns while still respecting the best REST ideas: resources are addressable, request semantics come from standard HTTP methods, server implementation details stay hidden, and every request carries enough information to be processed independently.

Use this decision rule:

- If the operation is about a resource lifecycle, use a standard method.
- If the operation changes application workflow but is not create, update, or delete, use a custom method.
- If clients need to discover possible next actions dynamically, include links or action metadata in the representation.
- If clients are generated from an OpenAPI or protobuf contract, keep the contract resource-oriented and stable.

## Resource Modeling

Resource modeling is the highest-leverage API design step. A clean resource model makes URLs, methods, permissions, documentation, and client libraries easier.

### Identify Resources

Start by listing the things users think about, not the tables or services we happen to run.

| Question                                      | Example answer       | API shape                                      |
| --------------------------------------------- | -------------------- | ---------------------------------------------- |
| What nouns do users manage?                   | Project, book, task  | `projects`, `books`, `tasks`                   |
| What owns what?                               | A project owns tasks | `/v1/projects/{project}/tasks/{task}`          |
| What can exist independently?                 | User                 | `/v1/users/{user}`                             |
| What is just an attribute of another thing?   | Book title           | Field on `Book`, not `/books/{book}/title`.    |
| What relationship needs its own metadata?     | Book membership      | A sub-resource such as `bookAuthors`.          |

AIP-121 recommends planning in this order: resources, relationships, resource schemas, and then methods. Do not mirror the database by default. The API is a product contract; the database is an implementation detail.

### Name Resources

Google AIPs use resource names, which are stable string identifiers such as:

```text
projects/acme/books/les-miserables
publishers/123/books/456
users/me
```

Use these naming rules:

- Collection segments are plural nouns: `books`, `projects`, `users`.
- Resource names usually alternate collection and ID: `projects/{project}/books/{book}`.
- IDs are strings. For user-provided IDs, prefer lowercase ASCII letters, numbers, and hyphens.
- Every resource has a `name` field containing its full resource name.
- Requests that act on one existing resource use a `name` field.
- Requests that create or list inside a collection use a `parent` field.
- References to other resources are strings containing resource names, not embedded copies of the referenced resource.

Use a single canonical parent. If a book belongs to a publisher and has an author, pick one as the parent and model the other as a field or filter:

```json
{
  "name": "publishers/123/books/456",
  "author": "authors/789",
  "title": "Les Miserables"
}
```

If a relationship has its own attributes, model it as its own resource:

```json
{
  "name": "publishers/123/books/456/authors/primary",
  "author": "authors/789",
  "role": "PRIMARY_AUTHOR"
}
```

### Design Fields

Keep fields predictable and self-explanatory.

| Field pattern       | Meaning                                                             |
| ------------------- | ------------------------------------------------------------------- |
| `name`              | Full resource name and canonical identifier.                        |
| `{resource}_id`     | User-provided ID during create, such as `book_id`.                  |
| `display_name`      | Human-readable label.                                               |
| `create_time`       | Server-set creation timestamp.                                      |
| `update_time`       | Server-set last update timestamp.                                   |
| `delete_time`       | Server-set deletion timestamp for soft-deleted resources.           |
| `etag`              | Concurrency token for conditional updates or deletes.               |
| `state`             | Output-only lifecycle state such as `CREATING`, `ACTIVE`, `FAILED`. |
| `request_id`        | Optional idempotency key for retry-safe mutating requests.          |
| `update_mask`       | Field mask for partial updates.                                     |
| `page_size`         | Maximum number of list results.                                     |
| `page_token`        | Token for the next list page request.                               |
| `next_page_token`   | Token returned when another page exists.                            |
| `filter`            | String filter expression for lists.                                 |
| `order_by`          | Sort expression for lists.                                          |

Mark field behavior in schema docs: required, optional, output-only, input-only, immutable, and identifier. Client tools use this information to generate better SDKs, CLIs, and validation.

## Standard Methods

Most APIs should be made from five standard methods: get, list, create, update, and delete. This is the core of AIP-121 and AIPs 131 through 135.

| Operation       | HTTP shape                                      | Body                     | Typical success result                   | Notes                                                   |
| --------------- | ----------------------------------------------- | ------------------------ | ---------------------------------------- | ------------------------------------------------------- |
| Get             | `GET /v1/{name=publishers/*/books/*}`           | None                     | `200 OK` with the resource.              | Every resource should support Get.                      |
| List            | `GET /v1/{parent=publishers/*}/books`           | None                     | `200 OK` with resources and page token.  | List should exist unless the resource is a singleton.   |
| Create          | `POST /v1/{parent=publishers/*}/books`          | Resource field.          | Created resource.                        | Include `{resource}_id` when users choose IDs.          |
| Update          | `PATCH /v1/{book.name=publishers/*/books/*}`    | Resource field.          | Updated resource.                        | Use `update_mask`; prefer `PATCH` over `PUT`.           |
| Delete          | `DELETE /v1/{name=publishers/*/books/*}`        | None                     | Empty response or deleted resource.      | Fail if child resources exist unless force is designed. |

For a simple HTTP JSON API, these routes are a good default:

```http
GET    /v1/publishers/{publisher}/books/{book}
GET    /v1/publishers/{publisher}/books?page_size=50&page_token=...
POST   /v1/publishers/{publisher}/books?book_id=les-miserables
PATCH  /v1/publishers/{publisher}/books/{book}?update_mask=title,rating
DELETE /v1/publishers/{publisher}/books/{book}
```

### Get

Use Get to return one resource by resource name:

```http
GET /v1/publishers/123/books/456
Accept: application/json
```

The response returns the resource:

```json
{
  "name": "publishers/123/books/456",
  "title": "Les Miserables",
  "rating": 5,
  "create_time": "2026-07-05T10:30:00Z",
  "update_time": "2026-07-05T10:30:00Z"
}
```

Rules:

- Do not use a request body with Get.
- Use `404 Not Found` when the resource does not exist and the caller is allowed to know that.
- Use `403 Forbidden` when the caller lacks permission, regardless of whether the resource exists.
- Make successful Get responses cacheable when the data and authorization model allow it.

### List

Use List to return a finite collection:

```http
GET /v1/publishers/123/books?page_size=50&page_token=abc&filter=rating%20%3E%3D%204&order_by=title
Accept: application/json
```

The response contains a repeated resource field and a `next_page_token`:

```json
{
  "books": [
    {
      "name": "publishers/123/books/456",
      "title": "Les Miserables",
      "rating": 5
    }
  ],
  "next_page_token": "def"
}
```

Rules:

- Add pagination from the first version of every collection-returning method.
- `page_size` is optional. If it is missing or zero, the server picks a documented default.
- If `page_size` is too large, coerce it to the documented maximum.
- If `page_size` is negative, return an invalid argument error.
- `page_token` is opaque. Clients must not parse it.
- The only reliable signal for the end of a collection is an empty `next_page_token`.
- Keep all request arguments the same when requesting the next page, except `page_size`.
- Use `filter` only when there is a real user need. It is easy to add later and hard to remove.
- Use `order_by` only when there is a real user need. Document the default order.

### Create

Use Create to add a new resource to an existing collection:

```http
POST /v1/publishers/123/books?book_id=les-miserables&request_id=8a0d1c4b-31af-4d5f-92e0-bc3b42f88f21
Content-Type: application/json
Accept: application/json

{
  "title": "Les Miserables",
  "rating": 5
}
```

The response returns the created resource:

```json
{
  "name": "publishers/123/books/les-miserables",
  "title": "Les Miserables",
  "rating": 5,
  "create_time": "2026-07-05T10:30:00Z",
  "update_time": "2026-07-05T10:30:00Z"
}
```

Rules:

- Use `POST` on the parent collection.
- Put the resource data in the body.
- Put user-chosen IDs in a separate `{resource}_id` parameter, not inside `name`.
- Return the fully populated resource, including server-set fields.
- Use `request_id` for retry-safe creates when duplicate creates would be harmful.
- If creation takes more than roughly 10 seconds, return a long-running operation.

### Update

Use Update to change an existing resource without unintended side effects:

```http
PATCH /v1/publishers/123/books/456?update_mask=title,rating
Content-Type: application/json
Accept: application/json
If-Match: "etag-value"

{
  "title": "Les Miserables: Revised Edition",
  "rating": 5
}
```

The response returns the updated resource:

```json
{
  "name": "publishers/123/books/456",
  "title": "Les Miserables: Revised Edition",
  "rating": 5,
  "etag": "\"new-etag-value\"",
  "update_time": "2026-07-05T11:00:00Z"
}
```

Rules:

- Prefer `PATCH` with `update_mask`.
- Use `PUT` only for full resource replacement when that behavior is stable forever.
- Field masks are relative to the resource: `title,rating`, not `book.title,book.rating`.
- Updates should not create unrelated side effects. Use a custom method for workflow actions.
- Use `etag` or `If-Match` when concurrent updates can overwrite each other.
- Ignore immutable fields when their value is unchanged; reject attempted changes.

### Delete

Use Delete to remove one resource:

```http
DELETE /v1/publishers/123/books/456
Accept: application/json
If-Match: "etag-value"
```

Rules:

- Do not use a request body with Delete.
- Return empty success for hard delete, or the resource when using soft delete.
- If the resource does not exist, return `404 Not Found`.
- If child resources exist, return a failed precondition unless the API explicitly supports cascading delete.
- Use a `force` option only when users understand the cascade.
- If deletion takes a long time, return a long-running operation.

## Custom Methods

AIP-136 exists for operations that do not fit the standard methods. Custom methods are useful, but they are also the easiest way to accidentally turn a resource API into an RPC API.

Use a custom method only when the operation is not naturally Get, List, Create, Update, or Delete.

| User intent              | Good design                                  | Why                                            |
| ------------------------ | -------------------------------------------- | ---------------------------------------------- |
| Archive a book.          | `POST /v1/publishers/123/books/456:archive`  | State transition, not a normal field update.   |
| Undelete a book.         | `POST /v1/publishers/123/books/456:undelete` | Reverses soft delete lifecycle.                |
| Validate a config.       | `POST /v1/projects/123/configs:validate`     | Validation is not creation.                    |
| Search across parents.   | `GET /v1/books:search?query=...`             | Query action over a collection-like surface.   |
| Export many resources.   | `POST /v1/publishers/123/books:export`       | Long-running action, not list.                 |

Rules:

- Prefer standard methods first.
- Use `POST` for mutating custom methods.
- Use `GET` only for custom methods that retrieve data and have no side effects.
- Put the custom verb after a colon: `:archive`, `:undelete`, `:validate`.
- Use lower camel case for multiword custom verbs: `:batchApprove`.
- Make the RPC or operation name `VerbNoun`, such as `ArchiveBook`.
- Do not include standard verbs inside custom method names. Avoid `CreateBookFromTemplate`; prefer `CreateBook` with a `template` field if it is still creation.
- Do not include prepositions such as `For` or `With` in method names.
- Do not include `Async`; use a long-running operation when the work is asynchronous.
- Keep the method stateless. The request includes everything needed to process it.

Bad custom methods usually reveal that the resource model is wrong:

```text
POST /v1/books/get
POST /v1/books/delete
POST /v1/books/updateTitle
POST /v1/books/createFromAuthorWithPublisher
```

Better resource-oriented alternatives are:

```text
GET    /v1/publishers/123/books/456
DELETE /v1/publishers/123/books/456
PATCH  /v1/publishers/123/books/456?update_mask=title
POST   /v1/publishers/123/books?author=authors/789
```

## HTTP Semantics

HTTP methods already carry meaning. Respecting that meaning makes APIs safer for clients, caches, gateways, observability, and automated tooling.

| Method    | Use for                                | Safe | Idempotent | Request body guidance                 |
| --------- | -------------------------------------- | ---- | ---------- | ------------------------------------- |
| `GET`     | Read a resource or collection.         | Yes  | Yes        | Avoid bodies. Use query parameters.   |
| `HEAD`    | Read metadata without representation.  | Yes  | Yes        | Avoid bodies.                         |
| `POST`    | Create or custom action.               | No   | No         | Use a body when sending data.         |
| `PATCH`   | Partial update.                        | No   | Not always | Use a patch document or field mask.   |
| `PUT`     | Full replacement at known URI.         | No   | Yes        | Body is the complete replacement.     |
| `DELETE`  | Delete the target resource.            | No   | Yes        | Avoid bodies.                         |

Status codes should be boring and consistent:

| Status                  | Use when                                                                 |
| ----------------------- | ------------------------------------------------------------------------ |
| `200 OK`                | Read, update, custom read, or custom action returns a representation.    |
| `201 Created`           | HTTP create returns a newly created resource and `Location`.             |
| `202 Accepted`          | Work has started and continues asynchronously.                           |
| `204 No Content`        | Delete or action succeeds and has no response body.                      |
| `400 Bad Request`       | Syntax or basic request shape is invalid.                                |
| `401 Unauthorized`      | Authentication is missing or invalid.                                    |
| `403 Forbidden`         | Caller is authenticated but lacks permission.                            |
| `404 Not Found`         | Resource does not exist or should not be revealed to the caller.         |
| `409 Conflict`          | Request conflicts with current resource state.                           |
| `412 Precondition Failed` | ETag or other precondition fails.                                      |
| `429 Too Many Requests` | Caller exceeds quota or rate limits.                                     |
| `500 Internal Server Error` | Unexpected server failure.                                           |
| `503 Service Unavailable` | Temporary overload or dependency outage.                               |

Use headers deliberately:

- `Content-Type` tells the server how to parse the request body.
- `Accept` tells the server what response format the client wants.
- `Authorization` carries credentials.
- `ETag` identifies a resource version.
- `If-Match` prevents lost updates.
- `Cache-Control` controls cache behavior.
- `Location` points to a created resource or operation.
- `Retry-After` tells clients when to retry after rate limits or temporary outages.

## Pagination, Filtering, And Ordering

Collections grow. Design for that from day one.

Pagination rules:

- Every collection-returning endpoint has `page_size`, `page_token`, and `next_page_token`.
- Page tokens are opaque. Clients store and replay them only.
- The server may return fewer items than requested.
- The server must return an empty `next_page_token` when the collection is complete.
- Do not add pagination later; that breaks clients that assumed a complete list.

Filtering rules:

- Use one `filter` string parameter.
- Document which fields can be filtered.
- Support a small, consistent grammar before inventing custom query structures.
- Prefer obvious comparisons: `rating >= 4`, `state = ACTIVE`, `create_time >= "2026-01-01T00:00:00Z"`.
- Return a clear invalid argument error for unsupported fields or malformed filters.

Ordering rules:

- Use one `order_by` string parameter.
- Use comma-separated fields: `title,create_time desc`.
- Default to ascending order unless `desc` is specified.
- Document the default order and any non-obvious ordering behavior.

## Errors

Error responses are part of the API contract. Clients should not need to parse English prose to understand what happened.

For Google-style APIs, use a `google.rpc.Status` shaped response. For plain HTTP JSON APIs, keep the same idea:

```json
{
  "error": {
    "code": 404,
    "status": "NOT_FOUND",
    "message": "Book 'publishers/123/books/456' was not found.",
    "details": [
      {
        "type": "ErrorInfo",
        "reason": "BOOK_NOT_FOUND",
        "domain": "library.example.com",
        "metadata": {
          "resource": "publishers/123/books/456"
        }
      }
    ]
  }
}
```

Rules:

- Use stable machine-readable status values.
- Keep `message` brief, specific, and actionable.
- Put structured data in `details`, not only in the text message.
- Do not leak database names, stack traces, hostnames, or internal service names.
- Use `403 Forbidden` when permission is denied, even if the resource might not exist.
- Use `404 Not Found` only when the caller is allowed to know absence.
- Document common errors per endpoint.

## Idempotency And Retries

Distributed systems fail between the client sending a request and receiving the response. Design mutating operations so clients can safely retry.

Rules:

- Use `request_id` on create and custom mutating requests that might be retried.
- Treat duplicate `request_id` values as the same logical request for a documented retention window.
- Return the original successful response when a duplicate request is detected.
- Keep `GET`, `HEAD`, `PUT`, and `DELETE` idempotent according to HTTP semantics.
- Use `ETag` and `If-Match` for updates and deletes that must not overwrite newer state.
- Return retry guidance for transient errors, especially `429` and `503`.

## Long-Running Operations

Do not keep a normal HTTP request open for work that takes a long time. AIP-151 gives a practical rule of thumb: around 10 seconds is long enough to consider a long-running operation.

Use this shape:

```http
POST /v1/publishers/123/books:export
Content-Type: application/json

{
  "format": "CSV",
  "request_id": "8a0d1c4b-31af-4d5f-92e0-bc3b42f88f21"
}
```

Return an operation resource:

```json
{
  "name": "operations/export-books-789",
  "done": false,
  "metadata": {
    "progress_percent": 10
  }
}
```

Clients poll the operation:

```http
GET /v1/operations/export-books-789
```

Rules:

- The operation has a stable `name`.
- The operation exposes `done`, `metadata`, `response`, and `error`.
- Errors that prevent the operation from starting return a normal error immediately.
- Errors during execution appear in the operation error field.
- Operation metadata should include useful progress, partial failure, or target resource information.
- Completed operations may expire after a documented retention period.

## Versioning And Compatibility

Versioning is mostly about promises. If clients integrate once, they should not break because the server made a minor improvement.

Rules:

- Put the major version in the path: `/v1`, `/v1beta`, `/v2`.
- Do not expose minor or patch versions such as `/v1.1` or `/v1.2.3`.
- Add optional fields, optional query parameters, new resources, and new methods when compatible.
- Do not add new required fields to existing requests.
- Do not remove, rename, or change the meaning of existing fields.
- Do not change a field type.
- Do not change resource name formats casually.
- If a breaking change is required, create a new major version.
- Run old and new major versions in parallel for a documented migration period.
- Deprecate before removing, and give users enough time to migrate.

Compatibility is more than wire format. It includes semantics. If old clients reasonably expect a list endpoint to return all items, adding pagination with a smaller default page changes behavior and can break them. This is why pagination must be present from the beginning.

## Security And Privacy

Security design belongs in the API contract, not only in middleware.

Rules:

- Use HTTPS only.
- Authenticate every non-public request.
- Authorize by resource, action, and caller.
- Validate every resource name and parent-child relationship.
- Never trust client-provided tenant, user, or organization IDs without checking access.
- Do not put secrets, tokens, passwords, or sensitive filters in URLs.
- Prefer request bodies for sensitive inputs when caching does not help.
- Redact secrets from logs, traces, metrics, and error messages.
- Use least-privilege OAuth scopes or permissions.
- Make destructive actions explicit and auditable.

## Documentation Template

Every endpoint should document the same set of facts. Consistency helps humans and generated tooling:

```markdown
### Create Book

Creates a book under a publisher.

`POST /v1/{parent=publishers/*}/books`

Request:

- `parent` is required. Format: `publishers/{publisher}`.
- `book_id` is required for caller-chosen IDs.
- Body is a `Book` without server-output fields.
- `request_id` is optional and makes retries idempotent.

Response:

- Returns the created `Book`.
- Returns a long-running operation if creation is asynchronous.

Errors:

- `400 INVALID_ARGUMENT` for malformed IDs or invalid fields.
- `403 PERMISSION_DENIED` when the caller cannot create books under the publisher.
- `404 NOT_FOUND` when the publisher does not exist and the caller may know that.
- `409 ALREADY_EXISTS` when `book_id` already exists.
```

## Design Checklist

Use this checklist before publishing a new API or endpoint.

### Resource Model

- The API is modeled around user-visible nouns.
- Every important resource has a stable resource name.
- Collection names are plural and consistent.
- Each resource has one canonical parent.
- Resource references use resource-name strings.
- The API does not expose database schema details.

### Methods

- Standard methods are used wherever they fit.
- Get and List have no request body.
- Create uses `POST` on the collection.
- Update uses `PATCH` and `update_mask`.
- Delete uses `DELETE` with no request body.
- Custom methods use `:verb` and do not duplicate standard methods.
- Mutating methods support idempotency when retry risk exists.

### Collections

- List methods are paginated from the first version.
- Page tokens are opaque.
- Filtering and ordering are documented.
- Defaults and maximums are documented.
- Soft-deleted resources are hidden by default unless `show_deleted` is provided.

### Schema

- Required, optional, output-only, input-only, immutable, and identifier fields are documented.
- Server-owned fields are output-only.
- Effective values use `effective_` fields when the server calculates a default.
- Timestamps, durations, quantities, and enums use consistent formats.
- Future enum values are considered in client behavior.

### Errors

- Errors use consistent HTTP status codes.
- Errors include machine-readable status or reason values.
- Messages are actionable and do not leak internals.
- Permission checks happen before existence checks when needed.
- Common errors are documented per endpoint.

### Compatibility

- No existing required fields are added.
- No fields are removed, renamed, or retyped.
- Existing semantics remain stable.
- A breaking change creates a new major version.
- Deprecations include a migration path and date.

## Source Map

These are the primary references behind this page:

- [Google AIP index](https://google.aip.dev/)
- [AIP-121 Resource-oriented design](https://google.aip.dev/121)
- [AIP-122 Resource names](https://google.aip.dev/122)
- [AIP-124 Resource association](https://google.aip.dev/124)
- [AIP-127 HTTP and gRPC Transcoding](https://google.aip.dev/127)
- [AIP-129 Server-Modified Values and Defaults](https://google.aip.dev/129)
- [AIP-131 Standard methods: Get](https://google.aip.dev/131)
- [AIP-132 Standard methods: List](https://google.aip.dev/132)
- [AIP-133 Standard methods: Create](https://google.aip.dev/133)
- [AIP-134 Standard methods: Update](https://google.aip.dev/134)
- [AIP-135 Standard methods: Delete](https://google.aip.dev/135)
- [AIP-136 Custom methods](https://google.aip.dev/136)
- [AIP-151 Long-running operations](https://google.aip.dev/151)
- [AIP-155 Request identification](https://google.aip.dev/155)
- [AIP-158 Pagination](https://google.aip.dev/158)
- [AIP-160 Filtering](https://google.aip.dev/160)
- [AIP-161 Field masks](https://google.aip.dev/161)
- [AIP-180 Backwards compatibility](https://google.aip.dev/180)
- [AIP-185 API Versioning](https://google.aip.dev/185)
- [AIP-190 Naming conventions](https://google.aip.dev/190)
- [AIP-193 Errors](https://google.aip.dev/193)
- [AIP-203 Field behavior documentation](https://google.aip.dev/203)
- [RFC 9110 HTTP Semantics](https://datatracker.ietf.org/doc/html/rfc9110)
- [RFC 5789 PATCH Method for HTTP](https://datatracker.ietf.org/doc/html/rfc5789)
- [Roy Fielding: REST APIs must be hypertext-driven](https://roy.gbiv.com/untangled/2008/rest-apis-must-be-hypertext-driven)
