# Naming and Package Organization in Java/Scala

A codebase should be organized by ownership, responsibility, and architectural boundary. Names must explain what a type represents, where it belongs, and how it should be used. Avoid generic names such as `Dto`, `Data`, `Model`, `Util`, `Helper`, `Manager`, and `Common`; they hide intent and create structural entropy.

## 1. Package Naming

Use packages to express architecture and business capability.

Preferred root format:

```text
com.company.product.<context>
```

Example:

```text
com.acme.backupcli
com.acme.billing.invoice
io.github.owner.toolname
```

For most applications, prefer feature-first structure:

```text
com.acme.billing.invoice.api
com.acme.billing.invoice.application
com.acme.billing.invoice.domain
com.acme.billing.invoice.persistence
```

Avoid technical-first structure:

```text
com.acme.controllers
com.acme.services
com.acme.repositories
com.acme.dtos
```

Technical-first packaging spreads one feature across unrelated folders. Feature-first packaging keeps the module cohesive.

Use lowercase singular package names:

```text
domain.model
domain.value
application.command
application.result
adapter.out.filesystem
cli.command
cli.output
```

Avoid:

```text
models
utils
helpers
common
base
misc
dtos
```

`common` or `shared` should be rare and reserved only for stable cross-cutting primitives, such as errors, time, money, or validation.

## 2. Recommended CLI Application Structure

For CLI applications, the CLI layer should only parse arguments, validate syntax, call application services, render output, and return exit codes.

Recommended structure:

```text
com.company.toolname
├── Main
├── cli
│   ├── command
│   ├── option
│   ├── output
│   └── error
├── application
│   ├── command
│   ├── query
│   ├── result
│   ├── service
│   └── port
│       ├── in
│       └── out
├── domain
│   ├── model
│   ├── value
│   ├── policy
│   └── exception
├── adapter
│   └── out
│       ├── filesystem
│       ├── process
│       ├── network
│       └── config
├── config
└── diagnostics
```

Responsibilities:

```text
cli          parses input and prints output
application  orchestrates use cases
domain       owns business rules
adapter      talks to OS, filesystem, process, network, database
config       wires dependencies
diagnostics  logging, tracing, debug reports, doctor checks
```

## 3. Class Naming by Responsibility

Name classes by role, not by implementation detail.

CLI layer:

```text
RootCommand
BuildCommand
ValidateCommand
DoctorCommand
BuildOptions
GlobalOptions
OutputFormat
ConsoleWriter
JsonOutputRenderer
TableOutputRenderer
ErrorRenderer
ExitCode
```

Application layer:

```text
BuildProjectCommand
ValidateConfigCommand
BuildProjectResult
ValidationResult
BuildProjectService
ValidateConfigService
```

Domain layer:

```text
Project
BuildPlan
ValidationIssue
ProjectPath
Version
ConfigValidationPolicy
InvalidProjectException
```

Adapter layer:

```text
FileSystem
DefaultFileSystem
ProcessRunner
DefaultProcessRunner
Environment
SystemEnvironment
ConfigReader
TomlConfigReader
YamlConfigWriter
```

The name must reveal the boundary. For example:

```text
BuildCommand
```

means the CLI command class.

```text
BuildProjectCommand
```

means the application use-case input.

These should not be the same type.

## 4. Avoid Generic DTO Naming

`DTO` is too vague. It says how data moves, not what role the type has.

Avoid:

```text
UserDto
ConfigDto
ProjectData
BuildModel
ResponseData
Payload
```

Prefer role-specific names:

```text
CreateUserRequest
UserResponse
BuildOptions
BuildArguments
BuildProjectCommand
BuildProjectResult
ProjectConfig
ValidationReport
```

For CLI applications, prefer:

```text
Options      raw CLI options
Arguments    normalized CLI input
Command      application input
Query        application read input
Result       application output
Report       human/machine-readable output
Issue        validation or diagnostic problem
```

## 5. Request, Response, Command, Query, Result

Use suffixes consistently.

Use `Request` and `Response` only at external boundaries such as HTTP or external APIs:

```text
CreateInvoiceRequest
InvoiceResponse
StripePaymentRequest
StripePaymentResponse
```

Use `Command` for write use cases:

```text
CreateInvoiceCommand
BuildProjectCommand
DeleteConfigEntryCommand
```

Use `Query` for read use cases:

```text
FindInvoiceQuery
ListProjectsQuery
ReadConfigQuery
```

Use `Result` for application outputs:

```text
CreateInvoiceResult
BuildProjectResult
ValidationResult
```

Use `Report` for formatted or diagnostic output:

```text
DoctorReport
ValidationReport
BuildReport
```

## 6. Domain Naming

Domain objects should have clean business names without technical suffixes.

Good:

```text
Project
Invoice
Payment
BuildPlan
Money
EmailAddress
ProjectPath
```

Bad:

```text
ProjectDto
InvoiceModel
PaymentData
BuildPlanObject
MoneyValue
```

Use value objects for meaningful primitives:

```text
ProjectPath
InvoiceId
UserId
Version
PortNumber
Timeout
```

Avoid passing raw `String`, `Int`, `Long`, or `Path` everywhere when the value has domain meaning.

## 7. Service Naming

A service name must describe a real capability.

Good:

```text
BuildProjectService
ValidateConfigService
InvoicePricingService
PaymentAuthorizationService
VersionResolver
PasswordHasher
TokenIssuer
ArchiveExtractor
```

Weak:

```text
BuildManager
ConfigProcessor
CommonService
ProjectHelper
FileUtil
GenericService
```

If the class performs one concrete behavior, name the behavior directly:

```text
PasswordHasher
InvoiceNumberGenerator
ConfigLoader
DependencyResolver
```

Do not use `Manager`, `Processor`, `Helper`, or `Util` unless there is no stronger domain name. In most cases, there is.

## 8. Mapper Naming

Mapper names should identify the boundary being mapped.

Good:

```text
UserApiMapper
InvoicePersistenceMapper
StripePaymentMapper
ConfigFileMapper
```

Weak:

```text
UserMapper
DtoMapper
CommonMapper
MapperUtils
```

A mapper should usually map between two clear boundaries:

```text
CLI options -> application command
domain model -> API response
persistence entity -> domain model
external response -> domain model
```

## 9. Ports and Adapters

Use ports for external dependencies and adapters for implementations.

Port names:

```text
FileSystem
ProcessRunner
Environment
Clock
HttpClient
CredentialStore
ConfigReader
ConfigWriter
PaymentGateway
```

Implementation names:

```text
DefaultFileSystem
NioFileSystem
DefaultProcessRunner
SystemEnvironment
JavaHttpClient
TomlConfigReader
YamlConfigWriter
StripePaymentGateway
```

Application and domain code should depend on ports, not concrete adapters.

Dependency direction:

```text
cli -> application -> domain
adapter -> application/domain ports
```

The domain must not depend on frameworks, CLI parsers, JSON libraries, HTTP clients, databases, or filesystem APIs.

## 10. Exception Naming

Exceptions should describe the failed rule or condition.

Good:

```text
InvalidConfigException
ProjectNotFoundException
UnsupportedOutputFormatException
FileSystemAccessException
ProcessExecutionException
```

Weak:

```text
BusinessException
ApplicationException
CommonException
SomethingWentWrongException
```

Use different exception types only when callers can react differently. Otherwise, prefer fewer, meaningful exceptions.

## 11. Scala-Specific Notes

Use the same architectural naming rules in Scala.

Prefer precise algebraic/domain names:

```scala
final case class ProjectPath(value: Path)
final case class BuildProjectCommand(path: ProjectPath)
final case class BuildProjectResult(report: BuildReport)
```

Use sealed traits/enums for closed sets:

```scala
enum OutputFormat:
  case Text, Json, Table
```

For effects and interpreters, keep names explicit:

```text
FileSystem[F]
LiveFileSystem
ProcessRunner[F]
LiveProcessRunner
ConfigReader[F]
TomlConfigReader
```

Avoid meaningless suffixes:

```text
ProjectOps
SyntaxHelper
CommonUtils
DataModel
```

Use `Ops` only for extension-method syntax modules, not for business services.

## 12. Final Naming Rules

A good name must answer three questions:

```text
What is it?
Which boundary owns it?
How should it be used?
```

Use this matrix:

```text
CLI command             BuildCommand
CLI options             BuildOptions
Normalized CLI input    BuildArguments
Application input       BuildProjectCommand
Application output      BuildProjectResult
Domain model            BuildPlan
Domain value object     ProjectPath
Domain policy           BuildValidationPolicy
Filesystem port         FileSystem
Filesystem adapter      DefaultFileSystem
Process port            ProcessRunner
Process adapter         DefaultProcessRunner
Output renderer         JsonOutputRenderer
Error renderer          ErrorRenderer
Exit code enum          ExitCode
```

The core rule: name classes by responsibility, packages by ownership, and modules by business capability. Good naming makes the architecture visible before reading method bodies.
