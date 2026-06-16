# Scala Code Style Guide

> **For AI Agents**: This guide provides clear, actionable rules for writing consistent Scala code in this project.

## Table of Contents

1. [Code Formatting](#code-formatting)
2. [Naming Conventions](#naming-conventions)
3. [Type Annotations](#type-annotations)
4. [Imports](#imports)
5. [Functions and Methods](#functions-and-methods)
6. [Classes and Objects](#classes-and-objects)
7. [Pattern Matching](#pattern-matching)
8. [Collections](#collections)
9. [Error Handling](#error-handling)
10. [Dependency Injection](#dependency-injection)
11. [OOP Design Patterns](#oop-design-patterns)
12. [Code Organization](#code-organization)
13. [Documentation](#documentation)
14. [Testing](#testing)

---

## Code Formatting

### Line Length
- **Maximum 120 characters** per line
- Break long method chains across multiple lines with `.` on the new line

```scala
// Good
val result = collection
  .filter(predicate)
  .map(transform)
  .sortBy(_.id)

// Bad
val result = collection.filter(predicate).map(transform).sortBy(_.id)
```

### Indentation
- **2 spaces** for indentation (no tabs)
- Align continuation lines with the opening delimiter

```scala
// Good
def complexMethod(
    param1: String,
    param2: Int,
    param3: Boolean
): Option[Result] = {
  // implementation
}

// Bad
def complexMethod(param1: String,
                 param2: Int,
                 param3: Boolean): Option[Result] = {
  // implementation
}
```

### Braces
- Use braces for multi-line blocks
- Omit braces for single-line expressions in `if`, `for`, etc.

```scala
// Good
if (condition) doSomething()

if (complexCondition) {
  doFirstThing()
  doSecondThing()
}

// Bad
if (condition) {
  doSomething()
}
```

### Blank Lines
- One blank line between methods
- Two blank lines between top-level class/object definitions
- No blank line after class/object declaration before first member

```scala
object MyService {
  def method1(): Unit = ???

  def method2(): Unit = ???
}


object AnotherService {
  def method(): Unit = ???
}
```

---

## Naming Conventions

### General Rules
- **UpperCamelCase** for classes, traits, objects: `UserService`, `HttpClient`
- **lowerCamelCase** for methods, vals, vars, parameters: `getUserById`, `maxRetries`
- **UpperCamelCase** for constants: `MaxTimeout`, `DefaultPort`
- **lowercase** for packages: `com.worxbend.services`
  - Use dots (`.`) for package separators, never underscores (`_`)
  - Example: `com.worxbend.config.sources` ✓ not `com.worxbend.config_sources` ✗

### Specific Guidelines

#### Packages
```scala
// Good
package com.worxbend.config.sources
package io.worxbend.user.repositories

// Bad
package com.worxbend.config_sources  // Python/snake_case style
package com.worxbend.configSources   // CamelCase in package names
```

#### Classes and Traits
```scala
// Good
class UserRepository
trait DatabaseConnection
object ConfigLoader

// Bad
class user_repository
trait database_connection
```

#### Methods and Values
```scala
// Good
def fetchUserData(userId: String): Future[User]
val connectionTimeout: Duration = 30.seconds
lazy val databasePool: ConnectionPool = createPool()

// Constants use UpperCamelCase
object Config {
  val MaxTimeout: Duration = 30.seconds
  val DefaultPort: Int = 8080
}

// Bad
def FetchUserData(user_id: String): Future[User]
val ConnectionTimeout: Duration = 30.seconds  // Wrong for regular val
val MAX_TIMEOUT: Duration = 30.seconds        // Wrong style for constants
```

#### Type Parameters
- Single uppercase letter for simple cases: `T`, `A`, `B`
- Descriptive names for domain types: `Result`, `Error`

```scala
// Good
def map[A, B](f: A => B): Option[B]
trait Cache[Key, Value]

// Bad
def map[TYPE1, TYPE2](f: TYPE1 => TYPE2): Option[TYPE2]
```

#### Acronyms
- Treat as regular words: `HttpClient`, `XmlParser` (not `HTTPClient`, `XMLParser`)
- Exception: 2-letter acronyms may be uppercase: `IO`, `DB`

```scala
// Good
class HttpService
class IOUtils

// Bad
class HTTPService
class ioUtils
```

---

## Type Annotations

### When to Annotate
- **Always** annotate public APIs (public methods, vals in traits/objects)
- **Always** annotate recursive functions
- **Omit** for obvious local values

```scala
// Good
def findUser(id: String): Option[User] = ???  // Public API
private val users = List.empty[User]          // Private, type clear from context

// Bad
def findUser(id: String) = ???                // Missing return type
private val users: List[User] = List.empty[User]  // Redundant
```

### Explicit Return Types
```scala
// Good
trait UserService {
  def getUser(id: String): Future[Option[User]]
  def createUser(user: User): Future[User]
}

// Bad
trait UserService {
  def getUser(id: String) = ???  // Inferred type in public API
}
```

---

## Imports

### Organization
Group imports in this order:
1. Standard library (`scala.*`, `java.*`)
2. Third-party libraries
3. Project imports

```scala
// Good
import scala.concurrent.Future
import scala.concurrent.duration.*

import zio.ZIO
import zio.ZLayer
import cats.effect.IO

import com.worxbend.services.UserService
import com.worxbend.models.User
```

### Wildcard Imports
- **Avoid** wildcard imports except for:
  - Implicit conversions/syntax packages
  - Duration DSL (`scala.concurrent.duration.*`)
  - Companion objects with many members

```scala
// Good
import zio.ZIO
import zio.ZLayer
import scala.concurrent.duration.*

// Acceptable
import cats.syntax.all.*
import zio.durationInt

// Bad
import com.worxbend.services.*
```

### Import Formatting
- Multiple imports from same package on separate lines
- Sort alphabetically within groups

```scala
// Good
import zio.Duration
import zio.ZIO
import zio.ZLayer

// Bad
import zio.{Duration, ZIO, ZLayer}
```

---

## Functions and Methods

### Method Length
- **Prefer** methods under 20 lines
- **Maximum** 50 lines; refactor if larger

### Single Responsibility
Each method should do one thing well.

```scala
// Good
def validateUser(user: User): Either[ValidationError, User] = ???
def saveUser(user: User): Future[Unit] = ???

// Bad
def validateAndSaveUser(user: User): Future[Either[ValidationError, Unit]] = {
  // validation logic
  // database logic
  ???
}
```

### Parameter Lists
- **Maximum 3-4 parameters** before considering a parameter object
- Use multiple parameter lists for currying or implicit parameters

```scala
// Good
def authenticate(username: String, password: String)(implicit ec: ExecutionContext): Future[User]

case class CreateUserRequest(username: String, email: String, role: Role)
def createUser(request: CreateUserRequest): Future[User]

// Bad
def createUser(username: String, email: String, password: String, role: Role, metadata: Map[String, String]): Future[User]
```

### Pure Functions
Prefer pure functions (no side effects) when possible.

```scala
// Good
def calculateTotal(items: List[Item]): BigDecimal =
  items.map(_.price).sum

// Acceptable (side effect encapsulated)
def saveUser(user: User): Future[Unit] = ???

// Bad (hidden side effect)
def processUser(user: User): User = {
  logger.info(s"Processing ${user.id}")  // Side effect
  sendEmail(user.email)                   // Side effect
  user.copy(processed = true)
}
```

---

## Classes and Objects

### Case Classes
- Use for immutable data structures
- Keep them simple (data only, minimal methods)

```scala
// Good
case class User(
    id: String,
    email: String,
    name: String,
    createdAt: Instant
)

// Bad (mutable)
case class User(
    var id: String,
    var email: String
)
```

### Companion Objects
- Use for factory methods, type class instances, constants

```scala
// Good
case class User(id: String, email: String)

object User {
  val Anonymous: User = User("", "")

  def fromRequest(req: CreateUserRequest): User =
    User(generateId(), req.email)
}
```

### Sealed Traits
- Use for ADTs (Algebraic Data Types)
- Define all implementations in same file

```scala
// Good
sealed trait Result[+E, +A]
object Result {
  final case class Success[A](value: A) extends Result[Nothing, A]
  final case class Failure[E](error: E) extends Result[E, Nothing]
}

// Bad (subclasses in different files)
sealed trait Result[+E, +A]
```

### Class Organization
Order members as follows:
1. Constructor parameters
2. `vals` and `vars`
3. Abstract members
4. Concrete members
5. Private/protected members last

```scala
class UserService(
    repository: UserRepository,
    cache: Cache
) {
  // Public vals
  val maxRetries: Int = 3

  // Public methods
  def getUser(id: String): Future[Option[User]] = ???

  def createUser(user: User): Future[User] = ???

  // Private methods
  private def validateUser(user: User): Boolean = ???

  private def sendNotification(user: User): Unit = ???
}
```

---

## Pattern Matching

### Formatting
- Align `case` statements
- Use `_` for unused variables

```scala
// Good
result match {
  case Success(value) => processValue(value)
  case Failure(error) => handleError(error)
}

user match {
  case User(id, _, name, _) => s"$name ($id)"
}

// Bad
result match {
case Success(value) => processValue(value)
case Failure(error) => handleError(error)
}
```

### Exhaustiveness
- Prefer sealed traits for exhaustive matching
- Avoid `case _ =>` when possible

```scala
// Good
sealed trait Status
case object Active extends Status
case object Inactive extends Status

def describe(status: Status): String = status match {
  case Active   => "active"
  case Inactive => "inactive"
  // Compiler ensures exhaustiveness
}
```

---

## Collections

### Prefer Immutable
Always use immutable collections unless performance requires mutability.

```scala
// Good
val users = List(user1, user2, user3)
val userMap = Map("id1" -> user1, "id2" -> user2)

// Bad
import scala.collection.mutable
val users = mutable.ListBuffer(user1, user2)
```

### Collection Methods
- Use collection methods over manual loops
- Chain operations for readability

```scala
// Good
val activeUserEmails = users
  .filter(_.isActive)
  .map(_.email)
  .sorted

// Bad
var emails = List.empty[String]
for (user <- users) {
  if (user.isActive) {
    emails = user.email :: emails
  }
}
emails = emails.sorted
```

### Options vs Null
- **Never** use `null`
- Use `Option` for optional values

```scala
// Good
def findUser(id: String): Option[User]

val maybeEmail = user.emailOpt.getOrElse("no-email@example.com")

// Bad
def findUser(id: String): User  // May return null
if (user != null) { ... }
```

---

## Error Handling

### Prefer Functional Approach
- Use `Either`, `Try`, or effect types (`ZIO`, `IO`) over exceptions
- Reserve exceptions for truly exceptional cases

```scala
// Good
def parseAge(s: String): Either[String, Int] =
  s.toIntOption.toRight(s"Invalid age: $s")

def loadConfig(): ZIO[Any, ConfigError, AppConfig] = ???

// Bad
def parseAge(s: String): Int =
  s.toInt  // Throws exception
```

### Error Types
Define specific error types for domain errors.

```scala
// Good
sealed trait UserError
case class UserNotFound(id: String) extends UserError
case class InvalidEmail(email: String) extends UserError

def getUser(id: String): Either[UserError, User]

// Bad
def getUser(id: String): Either[String, User]  // Stringly-typed errors
```

### Exception Handling
When handling exceptions, be specific.

```scala
// Good
try {
  dangerousOperation()
} catch {
  case _: FileNotFoundException => handleMissingFile()
  case _: SecurityException     => handleSecurityIssue()
}

// Bad
try {
  dangerousOperation()
} catch {
  case _: Exception => // Too broad
}
```

---

## Dependency Injection

### ZIO ZLayer (Preferred Pattern)
Use ZIO's `ZLayer` for dependency management.

```scala
// Service definition
trait UserRepository {
  def findById(id: String): ZIO[Any, DbError, Option[User]]
  def save(user: User): ZIO[Any, DbError, Unit]
}

// Implementation
case class UserRepositoryLive(dataSource: DataSource) extends UserRepository {
  override def findById(id: String): ZIO[Any, DbError, Option[User]] = ???
  override def save(user: User): ZIO[Any, DbError, Unit] = ???
}

// Layer definition
object UserRepositoryLive {
  val layer: ZLayer[DataSource, Nothing, UserRepository] =
    ZLayer.fromFunction(UserRepositoryLive.apply _)
}

// Module composition
object AppModule {
  val userService: ZLayer[UserRepository, Nothing, UserService] =
    ZLayer.fromFunction(UserService.apply _)

  val appLayer: ZLayer[Any, Nothing, UserService] =
    DataSource.layer >>> UserRepositoryLive.layer >>> userService
}
```

### Distage (Alternative Pattern)
Use distage `ModuleDef` for dependency injection.

```scala
// Service traits
trait UserService {
  def getUser(id: String): Future[Option[User]]
}

trait UserRepository {
  def findById(id: String): Future[Option[User]]
}

// Module definition
object AppModule {
  val module = new ModuleDef {
    make[UserRepository].from[UserRepositoryImpl]
    make[UserService].from[UserServiceImpl]
    make[AppConfig].from(AppConfig.load())
  }
}

// Usage
class UserServiceImpl(
    repository: UserRepository,
    config: AppConfig
) extends UserService {
  def getUser(id: String): Future[Option[User]] =
    repository.findById(id)
}
```

### Constructor Injection
Prefer constructor-based injection over setter/field injection.

```scala
// Good
class UserService(
    repository: UserRepository,
    cache: Cache,
    config: ServiceConfig
) {
  def getUser(id: String): Future[User] = ???
}

// Bad
class UserService {
  var repository: UserRepository = _  // Field injection
  var cache: Cache = _
}
```

### Interface Segregation
Keep interfaces small and focused.

```scala
// Good
trait UserReader {
  def findById(id: String): Future[Option[User]]
}

trait UserWriter {
  def save(user: User): Future[Unit]
  def delete(id: String): Future[Unit]
}

// Bad
trait UserRepository {
  def findById(id: String): Future[Option[User]]
  def save(user: User): Future[Unit]
  def delete(id: String): Future[Unit]
  def findAll(): Future[List[User]]
  def count(): Future[Int]
  def migrate(): Future[Unit]
  // Too many responsibilities
}
```

---

## OOP Design Patterns

### Repository Pattern
Encapsulate data access logic.

```scala
trait UserRepository {
  def findById(id: String): Future[Option[User]]
  def findByEmail(email: String): Future[Option[User]]
  def save(user: User): Future[Unit]
  def delete(id: String): Future[Unit]
}

class UserRepositoryImpl(db: Database) extends UserRepository {
  override def findById(id: String): Future[Option[User]] =
    db.run(Users.filter(_.id === id).result.headOption)
}
```

### Service Pattern
Encapsulate business logic.

```scala
trait UserService {
  def register(request: RegisterRequest): Future[Either[RegistrationError, User]]
  def authenticate(credentials: Credentials): Future[Either[AuthError, Session]]
}

class UserServiceImpl(
    repository: UserRepository,
    validator: Validator,
    hasher: PasswordHasher
) extends UserService {
  override def register(request: RegisterRequest): Future[Either[RegistrationError, User]] =
    for {
      _     <- validator.validate(request)
      hash  <- hasher.hash(request.password)
      user  = User(generateId(), request.email, hash)
      _     <- repository.save(user)
    } yield Right(user)
}
```

### Factory Pattern
Use companion objects for factories.

```scala
sealed trait Connection
object Connection {
  private class HttpConnection(url: String) extends Connection
  private class SocketConnection(host: String, port: Int) extends Connection

  def http(url: String): Connection = new HttpConnection(url)
  def socket(host: String, port: Int): Connection = new SocketConnection(host, port)
}
```

### Builder Pattern
Use for complex object construction.

```scala
case class HttpRequest private (
    method: String,
    url: String,
    headers: Map[String, String],
    body: Option[String]
)

object HttpRequest {
  def builder(): Builder = new Builder()

  class Builder {
    private var method: String = "GET"
    private var url: String = ""
    private var headers: Map[String, String] = Map.empty
    private var body: Option[String] = None

    def withMethod(m: String): Builder = { method = m; this }
    def withUrl(u: String): Builder = { url = u; this }
    def withHeader(key: String, value: String): Builder = {
      headers = headers + (key -> value)
      this
    }
    def withBody(b: String): Builder = { body = Some(b); this }

    def build(): HttpRequest = HttpRequest(method, url, headers, body)
  }
}
```

### Strategy Pattern
Use traits for polymorphic behavior.

```scala
trait PaymentStrategy {
  def processPayment(amount: BigDecimal): Future[PaymentResult]
}

class CreditCardPayment extends PaymentStrategy {
  override def processPayment(amount: BigDecimal): Future[PaymentResult] = ???
}

class PayPalPayment extends PaymentStrategy {
  override def processPayment(amount: BigDecimal): Future[PaymentResult] = ???
}

class PaymentProcessor(strategy: PaymentStrategy) {
  def process(amount: BigDecimal): Future[PaymentResult] =
    strategy.processPayment(amount)
}
```

### Decorator Pattern
Use for composable behavior enhancement.

```scala
trait ServerInfoProviderService {
  def getSystemInfo(): ZIO[Any, ReqflectServiceException, SystemInfo]
}

class DefaultServerInfoProviderService extends ServerInfoProviderService {
  override def getSystemInfo(): ZIO[Any, ReqflectServiceException, SystemInfo] = ???
}

class CacheAwareServerInfoProviderService(
    underlying: ServerInfoProviderService,
    cache: Cache[String, ReqflectServiceException, SystemInfo]
) extends ServerInfoProviderService {
  override def getSystemInfo(): ZIO[Any, ReqflectServiceException, SystemInfo] =
    cache.get("system-info").orElse(underlying.getSystemInfo())
}
```

---

## Code Organization

### Package Structure
Organize by feature/domain, not by layer.

```
com.worxbend.users/
  ├── User.scala                    // Domain model
  ├── UserService.scala             // Service interface
  ├── UserServiceImpl.scala         // Implementation
  ├── UserRepository.scala          // Repository interface
  ├── UserRepositoryImpl.scala      // Implementation
  ├── UserModule.scala              // DI module
  └── errors/
      └── UserError.scala           // Domain errors
```

### File Organization
- One public class/trait/object per file
- File name matches the public type name
- Companion object in same file as class

```scala
// User.scala
case class User(id: String, email: String)

object User {
  def fromRequest(req: CreateUserRequest): User = ???
}
```

### Module Organization
Group related layers in module objects.

```scala
object UserModule {
  val repository: ZLayer[DataSource, Nothing, UserRepository] =
    ZLayer.fromFunction(UserRepositoryImpl.apply _)

  val service: ZLayer[UserRepository & Config, Nothing, UserService] =
    ZLayer.fromFunction(UserServiceImpl.apply _)

  val routes: ZLayer[UserService, Nothing, UserRoutes] =
    ZLayer.fromFunction(UserRoutes.apply _)

  val fullLayer: ZLayer[DataSource & Config, Nothing, UserRoutes] =
    repository >>> service >>> routes
}
```

### Application Structure
```
src/main/scala/
  ├── com.worxbend.myapp/
  │   ├── MyApp.scala                   // Main entry point
  │   ├── AppModule.scala               // DI configuration
  │   ├── config/
  │   │   └── AppConfig.scala
  │   ├── domain/
  │   │   ├── User.scala
  │   │   └── Order.scala
  │   ├── services/
  │   │   ├── UserService.scala
  │   │   └── OrderService.scala
  │   ├── repositories/
  │   │   ├── UserRepository.scala
  │   │   └── OrderRepository.scala
  │   └── routes/
  │       ├── UserRoutes.scala
  │       └── OrderRoutes.scala
```

---

## Documentation

### Scaladoc
Document public APIs, complex logic, and non-obvious decisions.

```scala
/**
 * Manages user authentication and session handling.
 *
 * This service provides methods for user login, logout, and session validation.
 * All methods return effects in the ZIO context for composability.
 *
 * @param repository User data access layer
 * @param hasher Password hashing utility
 * @param config Authentication configuration
 */
class AuthService(
    repository: UserRepository,
    hasher: PasswordHasher,
    config: AuthConfig
) {
  /**
   * Authenticates a user with the provided credentials.
   *
   * @param credentials User's login credentials
   * @return Either an authentication error or a valid session
   */
  def authenticate(credentials: Credentials): ZIO[Any, AuthError, Session] = ???
}
```

### Comments
- Use comments sparingly
- Explain **why**, not **what**
- Prefer self-documenting code

```scala
// Good
// User emails must be validated before persisting to avoid downstream issues
// with the notification service that cannot handle malformed addresses
val validatedEmail = validateEmail(email)

// Bad
// Validate email
val validatedEmail = validateEmail(email)
```

---

## Testing

### Test Organization
Mirror production package structure in test directories.

```
src/
  ├── main/scala/com/worxbend/users/
  │   ├── UserService.scala
  │   └── UserRepository.scala
  └── test/scala/com/worxbend/users/
      ├── UserServiceSuite.scala
      └── UserRepositorySpec.scala
```

### Test Naming
- Use descriptive test names
- Follow pattern: `should [behavior] when [condition]`

```scala
class UserServiceSuite extends munit.FunSuite {
  test("should return user when valid ID is provided") {
    // test implementation
  }

  test("should return None when user does not exist") {
    // test implementation
  }

  test("should throw ValidationError when email is malformed") {
    // test implementation
  }
}
```

### Test Structure
Follow Arrange-Act-Assert pattern.

```scala
test("should calculate total price correctly") {
  // Arrange
  val item1 = Item("1", BigDecimal(10.50))
  val item2 = Item("2", BigDecimal(5.25))
  val cart = Cart(List(item1, item2))

  // Act
  val total = cart.calculateTotal()

  // Assert
  assertEquals(total, BigDecimal(15.75))
}
```

### Test Fixtures
Use traits for reusable test fixtures.

```scala
trait UserFixtures {
  val validUser: User = User("1", "test@example.com", "Test User")
  val adminUser: User = User("2", "admin@example.com", "Admin").copy(role = Admin)

  def createUser(id: String = "test", email: String = "test@example.com"): User =
    User(id, email, s"User $id")
}

class UserServiceSuite extends munit.FunSuite with UserFixtures {
  test("should process valid user") {
    val result = service.process(validUser)
    // assertions
  }
}
```

---

## Mill Build Tool Specifics

### Module Definition
Define modules clearly with explicit dependencies.

```scala
import mill._
import mill.scalalib._

object myapp extends ScalaModule {
  def scalaVersion = "3.7.1"

  def ivyDeps = Agg(
    ivy"dev.zio::zio:2.0.19",
    ivy"dev.zio::zio-http:3.0.0-RC2"
  )

  object test extends ScalaTests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit::1.0.0"
    )
  }
}
```

### Cross-Module Dependencies
```scala
object common extends ScalaModule {
  def scalaVersion = "3.7.1"
}

object services extends ScalaModule {
  def scalaVersion = "3.7.1"
  def moduleDeps = Seq(common)
}

object api extends ScalaModule {
  def scalaVersion = "3.7.1"
  def moduleDeps = Seq(common, services)
}
```

---

## Summary Checklist for AI Agents

When writing Scala code, ensure:

- [ ] Line length ≤ 120 characters
- [ ] 2-space indentation
- [ ] Proper naming: `CamelCase` for types, `camelCase` for values
- [ ] Public APIs have explicit type annotations
- [ ] Imports organized: stdlib → third-party → project
- [ ] Immutable data structures (case classes, immutable collections)
- [ ] `Option` instead of `null`
- [ ] Functional error handling (`Either`, `Try`, ZIO)
- [ ] Constructor-based dependency injection
- [ ] Small, focused methods (< 20 lines ideal)
- [ ] Pattern matching with sealed traits
- [ ] Companion objects for factories and constants
- [ ] Tests follow production structure and use descriptive names
- [ ] Documentation for public APIs and complex logic

---

**References:**
- [Twitter Scala Style Guide](https://twitter.github.io/effectivescala/)
- [Databricks Scala Guide](https://github.com/databricks/scala-style-guide)
- [Official Scala Style Guide](https://docs.scala-lang.org/style/)
- [ZIO Best Practices](https://zio.dev/guides/best-practices/)
