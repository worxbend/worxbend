---
title: Scala Code Style
---

## Overview

This guide defines how we write Scala in Worxbend. It is intentionally more than formatter rules: it describes API design, package boundaries, error handling, concurrency, testing, and the small naming choices that keep a codebase understandable after the original author has moved on.

The default stack is Scala 3 on Mill. New Scala code uses the `com.worxbend` package prefix, direct-style Scala, explicit boundaries, immutable domain models, and boring JVM engineering discipline. Prefer code that a senior engineer from another JVM language can read without learning local folklore.

## Non-Negotiables

These rules are deliberately strict because they prevent broad classes of production defects.

| Area          | Rule                                                                                 |
| ------------- | ------------------------------------------------------------------------------------ |
| Formatting    | Let Scalafmt format Scala, Mill, sbt, and Scala CLI files.                           |
| Packages      | New code uses `com.worxbend.<product>.<concept>`.                                    |
| Syntax        | Use Scala 3 indentation syntax for new code. Avoid braces unless required by syntax.  |
| Types         | Public and protected members have explicit result types.                             |
| Mutation      | No shared mutable state. Local mutation needs a narrow, measured reason.             |
| Errors        | Recoverable failures are values, usually `Either[DomainError, A]`.                   |
| Exceptions    | Throw only for defects or unrecoverable boundaries. Do not throw for normal control. |
| Nulls         | Do not use `null`; use `Option`, an ADT, or a validated domain type.                 |
| Concurrency   | Use Ox scopes, Flow, channels, actors, or atomic values. Do not invent primitives.   |
| Resources     | Ownership and release order must be explicit.                                       |
| Tests         | Each test verifies one behavior and avoids unrelated setup.                          |
| Warnings      | The codebase must compile with warnings treated as failures.                         |

## Tooling

The build is the authority. Local editor settings are useful only when they match the repository configuration.

### Build And Format

Use Mill from the repository root for normal work:

```bash
./mill __.compile
./mill __.test
./mill __.reformat
```

The repository currently uses Scala `3.7.1`, Scalafmt `3.9.0`, and warning-heavy compiler settings in the shared Mill modules. Do not weaken compiler options to make a warning disappear; fix the code or document a narrow, reviewed exception.

### Formatter Rules

Scalafmt owns whitespace, import sorting, trailing commas, redundant braces, and most line breaking. Do not hand-format against Scalafmt.

| Formatter setting | Local policy                                                                 |
| ----------------- | ---------------------------------------------------------------------------- |
| `maxColumn = 120` | This is a hard limit. Prefer shorter expressions when the code becomes dense. |
| Trailing commas   | Use trailing commas in multiline parameter, argument, and collection lists.   |
| Imports           | Let Scalafmt and Scalafix organize imports.                                   |
| Infix syntax      | Avoid infix syntax except where the formatter explicitly permits it.          |
| Braces            | Scala 3 indentation syntax is the default for new code.                       |

### Scalafix Rules

Scalafix encodes several semantic rules. Treat those rules as design feedback, not bureaucracy.

| Rule family             | Practical meaning                                                                    |
| ----------------------- | ------------------------------------------------------------------------------------ |
| `ExplicitResultTypes`   | Public and protected API signatures must stay stable during refactors.               |
| `DisableSyntax.noVars`  | Avoid mutation by default; use local mutation only when the rule is intentionally relaxed. |
| `DisableSyntax.noThrows` | Recoverable failures belong in return types.                                        |
| `DisableSyntax.noNulls` | `null` is not a domain value.                                                        |
| `DisableSyntax.noReturns` | A method result is the final expression.                                           |
| `DisableSyntax.noDefaultArgs` | Prefer explicit overloads, configuration types, or options over hidden defaults. |
| `NoValInForComprehension` | Extract named steps before the comprehension.                                      |
| `OrganizeImports`      | Imports should converge automatically across IDEs and CI.                            |

## Packages And Files

Package structure is design. A package name tells readers what concept they are inside and where a boundary begins.

### Package Names

Use `com.worxbend` for new code:

```scala
package com.worxbend.billing.invoices

import java.time.Instant

final case class InvoiceIssued(invoiceId: InvoiceId, issuedAt: Instant)
```

Package names are lowercase words separated by dots. Use concept names such as `billing`, `orders`, `auth`, `storage`, or `telemetry`. Avoid mechanism-only names such as `common`, `core`, `helpers`, `utils`, or `misc` unless the package is tiny and genuinely cross-cutting.

### File Names

For a primary type named `InvoiceService`, use `InvoiceService.scala`. A sealed family may live in one file when the family is normally read as one concept.

Use one file for this kind of small ADT:

```scala
package com.worxbend.orders

import java.time.Instant

sealed trait OrderState

object OrderState:
  final case class Draft(createdAt: Instant) extends OrderState
  final case class Confirmed(confirmedAt: Instant) extends OrderState
  final case class Cancelled(cancelledAt: Instant, reason: CancellationReason) extends OrderState
```

Do not create files named `Types.scala`, `Models.scala`, or `Helpers.scala`. If the only shared property is "these are types", split the file by concept.

### Visibility

Choose visibility when creating a type. Public is a promise; private is a design tool.

Use narrow top-level visibility when a type is implementation detail:

```scala
package com.worxbend.billing.reconciliation

import java.time.Instant

private[reconciliation] final case class ReconciliationRow(invoiceId: InvoiceId, amount: Money, seenAt: Instant)

private[billing] trait LedgerClient:
  def fetchEntries(since: Instant): Either[LedgerError, List[LedgerEntry]]

final class ReconciliationService(client: LedgerClient):
  def reconcile(since: Instant): Either[LedgerError, ReconciliationReport] =
    client.fetchEntries(since).map(ReconciliationReport.fromEntries)
```

Use default public visibility only for APIs intended to be used outside the current concept package. Start narrow, then widen after seeing a real call site.

## Naming

Names are part of the API. Optimizing for a few fewer characters is rarely worth the loss in searchability and review clarity.

### Type Names

Use `UpperCamelCase` for classes, traits, enums, objects, and type aliases. Names should be nouns or noun phrases unless the trait represents a capability such as `Readable`.

Prefer specific names:

| Prefer                              | Avoid                       |
| ----------------------------------- | --------------------------- |
| `ObjectStorageDocumentRepository`   | `OSDocRepo`                 |
| `PostgresInvoiceRepository`         | `DbInvoiceRepo`             |
| `InvoiceStatus`                     | `Status` outside a package. |
| `RetryPolicy`                       | `RetryHelper`               |
| `Clock`                             | `ClockTrait`                |

Do not suffix names with `Class`, `Trait`, or `Object`. The language construct should not leak into the domain name.

### Methods And Values

Use `lowerCamelCase` for methods, values, parameters, and fields. Avoid JavaBean-style getters.

| Intent                     | Prefer                                  | Avoid                          |
| -------------------------- | --------------------------------------- | ------------------------------ |
| Accessor                   | `invoice.total`                         | `invoice.getTotal`             |
| Predicate                  | `invoice.isPaid`                        | `invoice.paid`                 |
| Side effect                | `repository.save(invoice)`              | `repository.setInvoice(invoice)` |
| Domain query               | `repository.findById(invoiceId)`        | `repository.getInvoiceById(invoiceId)` |
| Public collection variable | `invoices`                              | `invoiceList`                  |

Use active verbs for side-effecting operations. Use noun-like names for pure values. Do not repeat the receiver in the method name: prefer `invoiceRepository.findById(id)` over `invoiceRepository.findInvoiceById(id)`.

### Constants

Constants live in the companion of the type that owns the concept. Use `UpperCamelCase`, not Java-style all caps:

```scala
package com.worxbend.http

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationInt

final class HttpRetryPolicy(maxAttempts: Int, baseDelay: FiniteDuration)

object HttpRetryPolicy:
  val DefaultMaxAttempts: Int = 3
  val DefaultBaseDelay: FiniteDuration = 250.millis
```

If a constant is business-specific, put it near the business concept. If it is deployment-specific, put it in typed configuration.

### Acronyms And Abbreviations

Treat acronyms as words: `HttpClient`, `XmlDocument`, `JsonCodec`. Short JVM terms such as `IO`, `DB`, and `JVM` are acceptable when they are clearer than the expanded form.

Avoid abbreviations in public API unless the abbreviation is more recognizable than the full word. Local variables in tiny scopes may be short when the meaning is obvious.

## Types And Domain Modeling

Scala code should make invalid states hard to represent. If a reviewer needs to ask "can this be empty?" or "what does true mean?", the type probably needs work.

### Explicit Result Types

Public and protected members need explicit result types:

```scala
package com.worxbend.users

import java.time.Instant

final class UserService(repository: UserRepository):
  def findUser(id: UserId): Either[UserLookupError, User] =
    repository.findById(id).toRight(UserLookupError.NotFound(id))

  def activeSince(user: User): Option[Instant] =
    user.activatedAt
```

Local values may rely on inference when the type is obvious. Add a type when the inferred type is wide, structural, path-dependent, or part of the reader's understanding.

### Opaque Types

Wrap primitive domain values. A raw `String`, `Int`, or `Boolean` rarely tells the truth at a boundary:

```scala
package com.worxbend.orders

opaque type OrderId = String

object OrderId:
  def from(value: String): Either[OrderIdError, OrderId] =
    val normalized = value.trim
    if normalized.nonEmpty then Right(normalized)
    else Left(OrderIdError.Empty)

  extension (id: OrderId) def value: String = id

enum OrderIdError:
  case Empty
```

Use opaque types for identifiers, quantities, codes, topics, tenant names, external IDs, ports, and money-like values. Validate at construction and keep invalid data out of the core domain.

### Enums And ADTs

Use Scala 3 enums or sealed traits for closed sets. Do not use `scala.Enumeration`:

```scala
package com.worxbend.subscriptions

import java.time.Instant

enum SubscriptionState:
  case Trial(startedAt: Instant)
  case Active(activatedAt: Instant)
  case Suspended(reason: SuspensionReason)
  case Cancelled(cancelledAt: Instant)
```

Use separate cases or separate types for different lifecycle states. Do not model lifecycle with many optional fields.

### Boolean Blindness

Avoid Boolean parameters and Boolean return values when the meaning is domain-specific.

Use a named enum instead:

```scala
package com.worxbend.notifications

import scala.concurrent.duration.FiniteDuration

enum DeliveryOutcome:
  case Delivered
  case Rejected

final class DeliveryMetrics:
  def record(outcome: DeliveryOutcome, duration: FiniteDuration): Unit =
    ()
```

Boolean is fine for simple predicates such as `Invoice#isPaid`. It is not fine for method arguments where the caller has to remember what `true` means.

### Option, Either, And Errors

Use `Option[A]` for absence when absence needs no extra explanation. Use `Either[E, A]` when the caller needs to know why an operation failed. Use a domain ADT for `E` once there is more than one meaningful reason.

Name nested values by shape when it improves readability:

| Shape                  | Naming pattern       |
| ---------------------- | -------------------- |
| `Option[Invoice]`      | `maybeInvoice`       |
| `Either[E, Invoice]`   | `invoiceOrError`     |
| `List[Invoice]`        | `invoices`           |
| `NonEmptyList[Invoice]` | `nonEmptyInvoices`  |

Avoid deep nesting such as `Either[Error, Option[A]]` unless the two dimensions mean different things. Often the clearer model is a richer error ADT or a separate result ADT.

## Functions And Methods

A function should either perform one logical operation or orchestrate a short sequence of named steps. Long functions often hide missing concepts.

### One Concern Per Function

Prefer an orchestration method whose body reads as a business process:

```scala
package com.worxbend.orders

import java.time.Clock
import java.time.Instant

final class CheckoutService(repository: OrderRepository, payments: PaymentGateway, clock: Clock):
  def checkout(orderId: OrderId, command: CheckoutCommand): Either[CheckoutError, Receipt] =
    for
      order <- repository.findDraft(orderId)
      priced <- priceOrder(order, command)
      payment <- payments.charge(priced.total, command.paymentMethod)
      receipt <- confirmOrder(priced, payment, clock.instant())
    yield receipt

  private def priceOrder(order: DraftOrder, command: CheckoutCommand): Either[CheckoutError, PricedOrder] =
    PricedOrder.from(order, command.discounts)

  private def confirmOrder(order: PricedOrder, payment: Payment, now: Instant): Either[CheckoutError, Receipt] =
    repository.confirm(order, payment, now)
```

Extract a step when naming the step makes the workflow easier to audit, even if the step has one call site.

### Accessors And Parentheses

Use no parentheses for pure accessor-like methods. Use parentheses for methods that compute, allocate, perform effects, or depend on time:

```scala
package com.worxbend.billing

import java.time.Instant

final class Invoice(private val lines: List[InvoiceLine], private val issuedAt: Instant):
  def total: Money =
    lines.foldLeft(Money.Zero)((sum, line) => sum + line.amount)

  def persist(repository: InvoiceRepository): Either[InvoiceError, Unit] =
    repository.save(this)
```

Keep this distinction consistent because it communicates whether calling the method is observational or operational.

### For-Comprehensions

Use for-comprehensions when sequencing the same effect or result type. Keep branches and calculations out of the generator list.

Extract conditional work before the comprehension:

```scala
package com.worxbend.access

final class AccessService(users: UserRepository, audit: AuditLog):
  def grant(command: GrantAccess): Either[AccessError, AccessGrant] =
    val requestedRole = normalizeRole(command.role)

    for
      user <- users.find(command.userId)
      grant <- AccessGrant.create(user, requestedRole)
      _ <- audit.recordGranted(grant)
    yield grant

  private def normalizeRole(role: Role): Role =
    role.canonical
```

Do not flatten nested monadic values after the fact. Sequence the operation that produces the inner value inside the comprehension.

### No Return Keyword

Do not use `return`. A method returns its final expression. Early exits should be modeled with `Either`, small extracted methods, or pattern matching.

## Imports

Imports should converge automatically. Manual import style fights are wasted review time.

### Ordering

Use the repository Scalafmt and Scalafix rules. The effective order starts with Worxbend imports, then project ecosystem libraries, third-party libraries, Scala, and Java.

Prefer explicit imports for normal APIs:

```scala
package com.worxbend.telemetry

import com.worxbend.config.TelemetryConfig

import izumi.logstage.api.IzLogger

import java.time.Instant

final class TelemetryReporter(config: TelemetryConfig, logger: IzLogger):
  def reportStarted(name: String, at: Instant): Unit =
    logger.info(s"Started ${name} at ${at}")
```

Wildcard imports need a reason: syntax modules, enum cases in tiny scopes, or libraries whose documented style depends on a prelude import. Do not wildcard-import broad application packages.

### Aliases

Use aliases when two libraries expose the same short name or when a Java type would confuse a Scala collection type:

```scala
package com.worxbend.interop

import java.util.{List as JList}

final class JavaUserClient:
  def fetchUsers(): JList[ExternalUser] =
    ???
```

Keep aliases local to the file. A package-wide alias usually means the boundary type needs a better name.

## Error Handling

Error handling is API design. It controls what callers can recover from and what operations can be retried safely.

### Recoverable Failures

If the caller can do something meaningful, return an error value:

```scala
package com.worxbend.documents

enum DocumentError:
  case NotFound(id: DocumentId)
  case InvalidState(id: DocumentId, state: DocumentState)
  case StorageUnavailable

final class DocumentService(repository: DocumentRepository):
  def publish(id: DocumentId): Either[DocumentError, PublishedDocument] =
    for
      draft <- repository.findDraft(id).toRight(DocumentError.NotFound(id))
      published <- draft.publish
      _ <- repository.save(published)
    yield published
```

Use error ADTs rather than strings when callers branch on the error. Strings are for human messages at the edge.

### Exceptions

Exceptions are for defects, violated invariants, failed application startup, or third-party APIs that cannot express failure as values. Catch the most specific exception at the boundary and convert it immediately.

Use a value boundary around exception-throwing libraries:

```scala
package com.worxbend.storage

import scala.util.control.NonFatal

final class ObjectStorageClient(raw: RawStorageClient):
  def read(key: StorageKey): Either[StorageError, Array[Byte]] =
    try Right(raw.readBytes(key.value))
    catch
      case StorageNotFoundException() => Left(StorageError.NotFound(key))
      case NonFatal(error)            => Left(StorageError.Unavailable(error.getMessage))
```

Do not log and rethrow except at a top-level process boundary. Duplicate logging produces noisy incidents and hides the first useful failure.

### Pattern Matching

Pattern match sealed families instead of probing with unsafe accessors. Exhaustiveness is one of Scala's main advantages over Java-style status codes:

```scala
package com.worxbend.invoices

final class InvoicePresenter:
  def label(state: InvoiceState): String =
    state match
      case InvoiceState.Draft       => "Draft"
      case InvoiceState.Sent        => "Sent"
      case InvoiceState.Paid        => "Paid"
      case InvoiceState.Cancelled   => "Cancelled"
```

Never use `Option#get`. If absence is impossible, use a type that proves it before reaching that code path.

## Collections And Data Flow

Choose collections by semantics. The type should tell readers how the data is used.

### Collection Choice

| Need                                       | Prefer                         |
| ------------------------------------------ | ------------------------------ |
| Ordered finite values                      | `List[A]`                      |
| Indexed access or append-heavy immutable data | `Vector[A]`                 |
| Optional value                             | `Option[A]`                    |
| At least one value                         | A non-empty collection type    |
| Binary data                                | `Array[Byte]` or `Chunk[Byte]` |
| Java interop                               | Java collection at the boundary only |
| Large or unbounded data                    | Streaming, paging, or `Iterator[A]` with owned lifetime |

Do not expose mutable collections from APIs. If a third-party library returns mutable data, copy it into an immutable domain value at the boundary.

### Large Data

Never materialize unbounded data into memory. Use paging, streaming, or Ox Flow pipelines.

For bounded parallel collection processing, prefer a Flow pipeline:

```scala
package com.worxbend.ingestion

import ox.flow.Flow

final class IngestionService(client: SourceClient, sink: EventSink):
  def ingest(batch: List[SourceId]): Unit =
    Flow.fromIterable(batch)
      .mapPar(8)(client.fetch)
      .runForeach(sink.write)
```

Document ordering requirements. Use unordered parallelism only when result order has no business meaning.

### Tuples

Tuples are fine for tiny local transformations. Do not expose tuples in public APIs when a named case class would explain the fields.

Use a named type at boundaries:

```scala
package com.worxbend.reports

import java.time.Instant

final case class UsageSample(accountId: AccountId, usedBytes: Long, capturedAt: Instant)

final class UsageReporter:
  def summarize(samples: List[UsageSample]): UsageSummary =
    UsageSummary.from(samples)
```

Avoid tuple accessors such as `_1` and `_2` in meaningful code. Pattern matching with names is clearer when a tuple is unavoidable.

## Configuration, Time, And External State

Hidden dependencies make tests unreliable and production behavior surprising.

### Typed Configuration

Configuration should be loaded once near application startup, validated into typed values, and passed through constructors.

Use duration and size types rather than raw numbers:

```scala
package com.worxbend.config

import scala.concurrent.duration.FiniteDuration

final case class HttpClientConfig(connectTimeout: FiniteDuration, readTimeout: FiniteDuration, maxConnections: Int)
```

Do not encode timeouts as unlabelled integers in code or config. `30.seconds` and `connectTimeout = 30.seconds` are readable; `30000` is not.

### Time And Randomness

Pass time, randomness, and ID generation explicitly. Do not hide them inside domain logic.

Inject a clock or generator:

```scala
package com.worxbend.audit

import java.time.Clock
import java.time.Instant
import java.util.UUID

final class AuditService(clock: Clock, nextId: () => UUID, repository: AuditRepository):
  def record(action: AuditAction): Either[AuditError, AuditEvent] =
    val event = AuditEvent(nextId(), action, Instant.now(clock))
    repository.save(event).map(_ => event)
```

This makes tests deterministic and makes production behavior explicit.

### Secrets

Secrets never belong in source code, tests, logs, error messages, or generated docs. Wrap sensitive values in a type whose display behavior is redacted.

## Concurrency And Lifecycle

Concurrency is an ownership problem before it is a performance problem. Every fork, channel, backend, and file handle needs a parent scope and a shutdown path.

### Use Ox

Use Ox for new direct-style concurrency:

| Need                               | Default pattern                         |
| ---------------------------------- | --------------------------------------- |
| Small fixed parallel work          | Ox parallel helpers in a local scope.   |
| Collection or stream processing    | `Flow`.                                 |
| Mailbox or producer-consumer queue | `Channel[A]`.                           |
| Serialized mutable object          | `Actor`.                                |
| App daemon process                 | Daemon fork tied to the app scope.      |
| Worker that must drain             | User fork plus explicit channel close.  |

Avoid raw threads, ad hoc executors, blocking queues, lifecycle booleans, and hand-rolled schedulers. Use Java concurrency primitives only for small atomic state or when bridging a foreign API.

### Scope Ownership

Accept an Ox scope only when work or resources must attach to the caller's lifetime. Otherwise create a local supervised scope and join before returning.

Use a factory for app-owned workers:

```scala
package com.worxbend.workers

import ox.Ox
import ox.channels.Channel
import ox.forkUserDiscard
import ox.repeatWhile

final class NotificationWorker private[workers] (mailbox: Channel[NotificationCommand]):
  def submit(command: NotificationCommand): Unit =
    mailbox.send(command)

  def close(): Unit =
    mailbox.doneOrClosed().discard

object NotificationWorker:
  def start(handler: NotificationHandler)(using Ox): NotificationWorker =
    val mailbox = Channel.bufferedDefault[NotificationCommand]
    forkUserDiscard:
      repeatWhile:
        mailbox.receiveOrClosed() match
          case command: NotificationCommand =>
            handler.handle(command)
            true
          case ChannelClosed.Done =>
            false
          case ChannelClosed.Error(error) =>
            throw error
    NotificationWorker(mailbox)
```

Constructors should not capture an Ox capability. A factory can start the worker and return a plain value with a clear close operation.

### Resource Safety

Acquire resources inside the scope that owns them and release them in reverse order. Prefer Ox resource helpers for scoped resources and `scala.util.Using` for a small local Java-style resource.

Make ownership visible:

```scala
package com.worxbend.app

import ox.Ox
import ox.useCloseableInScope

final case class Dependencies(database: Database, httpClient: HttpClient)

object Dependencies:
  def create(config: AppConfig)(using Ox): Dependencies =
    val database = useCloseableInScope(Database.connect(config.database))
    val httpClient = useCloseableInScope(HttpClient.create(config.http))
    Dependencies(database, httpClient)
```

Do not return a resource whose lifetime is tied to a scope that already ended. Do not rely on finalizers.

## HTTP, JSON, And Boundaries

Boundaries translate between the outside world and the domain. Keep that translation explicit.

### DTOs And Domain Types

Use DTOs for wire formats and domain types for business rules. Convert at the boundary:

```scala
package com.worxbend.orders.http

import sttp.tapir.Schema

final case class CreateOrderRequest(customerId: String, items: List[CreateOrderItem]) derives Schema

final class OrderRoutes(service: OrderService):
  def create(request: CreateOrderRequest): Either[OrderHttpError, OrderResponse] =
    for
      command <- CreateOrderCommand.fromRequest(request)
      order <- service.create(command).left.map(OrderHttpError.fromDomain)
    yield OrderResponse.fromDomain(order)
```

Do not let external JSON quirks leak into the domain. If an upstream uses weak strings, normalize once at the adapter boundary.

### JSON Codecs

For new Tapir endpoints using jsoniter-scala, make the JSON representation agree with the Tapir schema. This matters most for enums and opaque identifiers.

Keep codec and schema definitions near the DTO:

```scala
package com.worxbend.orders.http

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.CodecMakerConfig
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import sttp.tapir.Schema

enum OrderStatus:
  case Draft, Confirmed, Cancelled

object OrderStatus:
  given Schema[OrderStatus] = Schema.derivedEnumeration[OrderStatus].defaultStringBased

final case class OrderResponse(id: String, status: OrderStatus) derives Schema

object OrderResponse:
  given JsonValueCodec[OrderResponse] =
    JsonCodecMaker.make(CodecMakerConfig.withDiscriminatorFieldName(None))
```

If a body is a list at the top level, provide a codec for the list type as well as the element type.

### Dependency Injection

Prefer constructor injection. Dependencies should be visible from the type signature and easy to replace in tests.

Use a small dependency wiring module near application startup:

```scala
package com.worxbend.app

final case class Services(orders: OrderService, invoices: InvoiceService)

object Services:
  def create(repositories: Repositories, clients: Clients): Services =
    Services(
      orders = OrderService(repositories.orders, clients.payments),
      invoices = InvoiceService(repositories.invoices),
    )
```

Do not use field injection. Avoid inheritance-based wiring patterns. Composition is easier to test and change.

## Testing

Tests should make behavior obvious. A test that verifies everything usually explains nothing.

### Unit Tests

Each unit test covers one scenario. Name the behavior, set up only what it needs, and assert the result in domain terms.

Use in-memory fakes when they make the behavior clearer than mocks:

```scala
package com.worxbend.orders

import munit.FunSuite

final class CheckoutServiceSuite extends FunSuite:
  test("checkout confirms a draft order after payment succeeds"):
    val orders = InMemoryOrderRepository.withDraft(OrderFixtures.draft)
    val payments = PaymentGateway.succeeding
    val service = CheckoutService(orders, payments, TestClock.Fixed)

    val result = service.checkout(OrderFixtures.DraftId, CheckoutCommandFixtures.valid)

    assertEquals(result.map(_.status), Right(OrderState.Confirmed))
```

Mocking is acceptable for awkward external protocols, but a small fake often documents the contract better.

### Integration Tests

Integration tests verify boundaries: database mapping, HTTP routing, serialization, broker wiring, object storage behavior, and external client adapters. Keep business rules testable in unit tests so integration tests stay focused and affordable.

Prefer this split:

| Test type       | Owns                                                         |
| --------------- | ------------------------------------------------------------ |
| Unit            | Domain rules, validation, state transitions, error mapping.  |
| Integration     | Real database, HTTP server/client, serialization, migration. |
| End-to-end      | A small number of critical workflows.                        |

Do not hide flaky timing behind sleeps. Use test clocks, controlled queues, explicit latches, or deterministic Ox scopes.

## Documentation And Comments

The best code needs fewer comments because names and types carry intent. Comments are still valuable when they explain a decision the code cannot express.

### Comments

Write comments for:

- Non-obvious business rules.
- External system quirks.
- Performance constraints.
- Security constraints.
- Concurrency or resource ownership assumptions.

Do not narrate the syntax. A comment such as "increment counter" above code that increments a counter adds noise.

### Scaladoc

Use Scaladoc for public APIs that are reused across modules or published outside the immediate application. Document contract, failure behavior, resource ownership, and concurrency expectations.

Prefer this style:

```scala
package com.worxbend.storage

/** Reads immutable objects from object storage.
  *
  * Implementations return `StorageError.NotFound` when the key is absent and
  * `StorageError.Unavailable` when the backing service cannot be reached.
  */
trait ObjectReader:
  def read(key: StorageKey): Either[StorageError, Array[Byte]]
```

Do not document private methods unless the private method encodes a subtle algorithm or boundary condition.

## Review Checklist

Use this checklist before opening a pull request or asking another engineer to review Scala code.

### Design

- The package name describes a concept, not a mechanism.
- New code uses the `com.worxbend` package prefix.
- Public types and methods are intentionally public.
- Domain invariants are encoded in types.
- Raw primitives do not cross important domain boundaries.
- No Boolean argument requires the caller to remember what `true` means.
- Effects, time, randomness, and external clients are explicit dependencies.

### Implementation

- Scalafmt and Scalafix converge without manual cleanup.
- Public and protected members have explicit result types.
- Functions do one thing or orchestrate named steps.
- Recoverable errors are represented as values.
- Exceptions are converted at boundaries or allowed to fail the processing unit.
- Collections are bounded, streamed, or paged according to data size.
- Resource acquisition and release ownership is visible.
- Concurrency uses Ox or a documented boundary primitive.

### Tests

- Each test covers one scenario.
- Business rules are testable without external infrastructure.
- Integration tests focus on real boundaries.
- Time and randomness are deterministic in tests.
- Failure paths are tested as deliberately as success paths.

## Migration Notes

Existing code may not satisfy every rule. Do not create churn-only rewrites. When touching a file, improve the code in the direction of this guide within the scope of the change.

Use this order of priority:

1. Keep behavior correct.
2. Preserve public compatibility unless a breaking change is intentional.
3. Add or update tests around the behavior being changed.
4. Move new code toward `com.worxbend` packages and this style guide.
5. Leave unrelated cleanup for a separate change.

Style work should make future changes safer. If a refactor does not improve readability, type safety, testability, or operational confidence, do not include it in the same pull request.
