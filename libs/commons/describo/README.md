# describo

Configurable `toString` derivation for Scala 3, built on
[Magnolia](https://github.com/softwaremill/magnolia). You describe *what* a value should look
like with a `Configuration`, and describo derives the rendering from the type's structure — no
hand-written `toString`, no reflection at render time, and no way for a `@Redacted` field to leak.

- Scala 3 only (3.8.4).
- One dependency: `com.softwaremill.magnolia1_3::magnolia`.
- Sibling module [`dscrbo`](../dscrbo/README.md) produces **byte-identical output** from an inline
  macro with *zero* dependencies. Pick describo when you want typeclass composition and instances
  for third-party types; pick dscrbo when you cannot take a dependency.

## Coordinates

```scala
mvn"io.worxbend::describo:0.1.0-SNAPSHOT"   // groupId io.worxbend
import com.worxbend.describo.*              // Scala package com.worxbend.describo
```

The publishing organization is `io.worxbend` while the Scala package is `com.worxbend.describo`.
That is intentional, not an oversight: `io.worxbend` is this repository's established groupId, and
`com.worxbend.<product>.<concept>` is the mandated package prefix for new code. Please do not
"fix" one to match the other.

`dscrbo` deliberately lives in a **different leaf package**, `com.worxbend.dscrbo`. Both artifacts
define `Configuration`, `annotations.Redacted` and `annotations.Excluded`, so distinct leaf
packages are what makes the two co-installable on one classpath.

## Usage

### The typeclass

```scala
import com.worxbend.describo.*
import com.worxbend.describo.annotations.*

final case class Account(
    @Redacted password: String,
    @Excluded internalId: Long,
    name: String,
    roles: List[String],
) derives Printable

val account = Account("hunter2", 17L, "Ada", List("Admin", "User"))

summon[Printable[Account]].asString(account)
// Account(password = <redacted>, name = "Ada", roles = ["Admin", "User"])

summon[Printable[Account]].asString(account)(using Configuration(useTypeNames = true))
// Account(password: String = <redacted>, name: String = "Ada", roles: List = ["Admin", "User"])
```

`asString` takes the configuration as a `using` parameter that defaults to `Configuration.default`.
Pass one explicitly, or put a `given Configuration` in scope.

### The `AutoToString` mixin

```scala
given Configuration = Configuration(multiline = true)

final case class Account(@Redacted password: String, name: String)
    extends AutoToString derives Printable

println(Account("hunter2", "Ada"))
// Account(
//   password = <redacted>,
//   name = "Ada"
// )
```

The mixin's `Configuration` is `scala.compiletime.deferred`: it is resolved **once, at the
class-definition site**. A type that mixes in `AutoToString` therefore renders with one fixed
configuration for its whole life. To vary the configuration per call, call the typeclass directly.

Both mixin members are named `describoPrintable` and `describoConfiguration` and are `protected`.
They share a namespace with your fields, so short names are not safe: an earlier version declared
`given p` and `given c`, and any case class with a field named `p` or `c` failed to compile.

## Annotations

`@Redacted`, `@Redacted("custom")`, `@Excluded` and `@transient` (an alias of `@Excluded`, kept for
compatibility). Exclusion beats redaction; the value of an omitted or redacted field is never
dereferenced, so a `null` secret is safe.

```scala
final case class Account(
    id:                 Long,
    @Redacted password: String,
    @Excluded internal: String,
) derives Printable
// Account(id = 1, password = <redacted>)
```

> **The full annotation semantics — precedence, repeated `@Redacted`, value classes — are specified in
> [`docs/libraries/tostring-rendering-spec.md`](../../../docs/libraries/tostring-rendering-spec.md).**

## Configuration reference

`Configuration` carries sixteen options: field names, type names, qualified names, the five affixes,
three separators, and the multiline layout with its threshold.

```scala
given Configuration = Configuration(multiline = true, useTypeNames = true)
```

Two that surprise people:

- **`multilineIfFieldsAreGreaterOrEqual <= 0` disables the threshold** — it does not mean "always
  multiline". Use `multiline = true` for that, and `-1` as the idiomatic "always one line".
- **`fieldsSeparator` is used verbatim** on a single line; in multiline its trailing whitespace is
  stripped before the newline.

> **The full option table with defaults and the field layout formula are specified in
> [`docs/libraries/tostring-rendering-spec.md`](../../../docs/libraries/tostring-rendering-spec.md).**
> That page is normative: this README describes how to *use* `describo`, not what the output is.

## Output format

Strings are quoted and escaped, collections render as `[a, b]`, maps as `["k" -> "v"]`, `Option` as
`Some(x)`/`None`, and `null` as the bare word `null` everywhere — including as a collection element, a
map key or an `Option` payload. Under `useTypeNames` the **declared** type is printed, never the
runtime class, so a `List` field reports `List` and not `$colon$colon`.

> **The complete value, escaping, `null`, type-name and layout rules are specified in
> [`docs/libraries/tostring-rendering-spec.md`](../../../docs/libraries/tostring-rendering-spec.md),
> and enforced for both engines by `libs/commons/describo-tck`.**

## Supported types

Instances ship for: `String`, `Char`, `Int`, `Long`, `Short`, `Byte`, `Double`, `Float`, `Boolean`,
`BigInt`, `BigDecimal`, `java.lang.Integer`, `java.lang.Character`, `java.time.{LocalDate,
LocalTime, Instant, Duration, Period, ZonedDateTime, OffsetDateTime, OffsetTime}`, `Option`,
`Iterable`, `Seq`, `IndexedSeq`, `List`, `Vector`, `Set`, `Array`, `Map`, and `java.util.{List,
ArrayList, LinkedList, Set, HashSet, Map, HashMap}`.

The typeclass is **invariant**, which is what guarantees that a field declared `List[String]` picks
the `List` instance and not the `Iterable` one. The flip side is that only the exact declared type
resolves: a field declared as some other type (`ArrayDeque`, `TreeMap`, `UUID`, …) needs its own
instance.

### Writing your own instance

`Printable` has two members — the `printedType` and the `asString` extension — but you do not have
to implement them by hand. Four public factories fill both in and give you this module's escaping,
`null` handling and element separator for free. Each takes the simple and the fully qualified name
of the type, which is what `useTypeNames` and `fullyQualifiedClassName` print.

```scala
import com.worxbend.describo.*
import scala.jdk.CollectionConverters.*

// A scalar rendered by a plain function. The string is used verbatim, so quote it yourself if the
// type should appear quoted.
given Printable[java.util.UUID] =
  Printable.instance("UUID", "java.util.UUID")(_.toString)

// A container rendered as [a, b, c]; the elements go through their own instance.
given Printable[java.util.ArrayDeque[String]] =
  Printable.collection("ArrayDeque", "java.util.ArrayDeque")(_.asScala.iterator)

// A container rendered as [k -> v]; keys and values go through their own instances.
given Printable[java.util.TreeMap[String, Int]] =
  Printable.mapping("TreeMap", "java.util.TreeMap")(_.asScala.iterator)

// A wrapper rendered as its payload but named after the wrapper.
given Printable[UserId] =
  Printable.valueClass("UserId", "com.example.UserId")(_.value)
```

`PrintableFactorySuite` in this module's tests is exactly the code above, so the snippet cannot rot.

### Value classes

Scala 3 synthesises no `Mirror` for a value class, so `derives Printable` cannot be used on one —
Magnolia never sees it, and `final case class UserId(value: String) extends AnyVal derives Printable`
does not compile. Give it an instance explicitly:

```scala
final case class UserId(value: String) extends AnyVal
object UserId:
  given Printable[UserId] = Printable.valueClass("UserId", "com.example.UserId")(_.value)
```

The wrapper renders as its payload (`"u1"`), but is *named* after the wrapper, so a field of type
`UserId` prints `id: UserId = "u1"` under `useTypeNames`. `@Redacted` on a *field* of value-class
type is honoured without unwrapping.

Because the instance reads the payload directly, nothing ever inspects the value class's own
parameter: an `@Excluded`, `@transient` or `@Redacted` written **on that parameter** has no effect.
Annotations belong on the fields of the enclosing case class. This is also why `@Excluded` on a sole
value-class parameter cannot omit anything — there would be nothing left to print.

## Known limitations

- **Nested renders are not re-indented.** In multiline layout a nested case class is inserted
  verbatim, so its own lines are not indented relative to the parent. Use a single-line
  configuration for deeply nested values.
- **Nesting depth is bounded by the stack.** Rendering is a recursive descent, one JVM frame per
  level, so a value nested a few hundred levels deep throws `StackOverflowError` — at roughly the
  same depth the case class's own generated `toString` would. Breadth is unaffected: a collection of
  any size renders iteratively. There is no configurable depth cap; if you render arbitrarily deep
  recursive structures, bound them yourself before rendering.
- **Value classes cannot be auto-derived** (see above), so `Printable.valueClass` is the supported
  route. `join` carries no value-class branch, because Scala 3 gives a value class no `Mirror` and
  Magnolia therefore never hands `join` one; `PrintableStructureSuite` pins that with an
  `assertDoesNotCompile`, so the day it changes the test fails rather than the behaviour drifting.
- **Enum cases under `fullyQualifiedClassName`** carry Magnolia's `TypeInfo`, which reports the
  enclosing *package* rather than the enclosing enum, so `Colour.Red` prints as
  `com.example.Red`. Simple names are unaffected. **This is one of the two places `describo` and
  `dscrbo` disagree**: the sibling macro reads the case symbol directly and prints
  `com.example.Colour.Red`, which is the more useful spelling. Fixing it here would mean threading the
  parent's name through `split` into every child instance — a change to the `Printable` interface for a
  spelling that only appears under a non-default flag. Both behaviours are pinned by
  `KnownDivergenceSuite` in each module.
- **Set and Map iteration order** is the insertion order only up to four elements; beyond that
  Scala switches to a hashed representation. Sort before rendering if you need stable output.

## Parity with `dscrbo`, and the two places it stops

`describo` and [`dscrbo`](../dscrbo) are two implementations of one specification. For the same input
and an equivalent `Configuration` they produce **byte-identical** output, and that is enforced rather
than promised: `libs/commons/describo-tck` holds the specification as data — a catalogue of
`(fixture, configuration, expected string)` obligations — and both modules run it through a thin
adapter. Neither *main* module depends on the kit, so `dscrbo`'s zero-dependency guarantee is intact.

Two differences are deliberate and are pinned by `KnownDivergenceSuite` on both sides, so neither can
quietly become three:

1. **Enum case qualified names.** `describo` prints `com.example.Red`, `dscrbo` prints
   `com.example.Colour.Red`. See [Known limitations](#known-limitations). Simple names agree.
2. **Generic case classes.** `describo` derives `Box[Int]` from `Box[A] derives Printable` without
   ceremony, because its typeclass has real instances for the built-in types a type parameter resolves
   to. `dscrbo` cannot: its synthesised `derived$Describe[A]` needs a `Describe[A]`, and that module
   ships no per-type instances by design. **This is a capability `describo` has and `dscrbo` does
   not.**

Everything else — escaping, `null`, collection and map brackets, `Option`, annotation precedence
including repeated `@Redacted`, value classes, case objects, sealed families, the multiline threshold
and every formatting knob — is covered by the shared catalogue and must match exactly.

## Compatibility

Version `0.1.0-SNAPSHOT` has never been published, and the following source-breaking changes were
made together, on purpose, before the first release:

- The package moved from `io.worxbend.describo` to `com.worxbend.describo`.
- `Configuration` and `annotations.{Redacted, Excluded}` moved out of `object Printable` and into
  the package (`com.worxbend.describo.Configuration`, `com.worxbend.describo.annotations.Redacted`).
- `Printable.TypeAliases` was **deleted**. It mapped runtime class names such as `$colon$colon` and
  `Map2` back to friendly names; type names are now taken from the declared type, so there is
  nothing left to map.
- `Printable` gained an abstract member, `printedType`. Hand-written instances must supply it — the
  four factories under "Writing your own instance" supply it for you.
- `Configuration.fieldsSeparator`'s default changed from `","` to `", "`. The field is now actually
  read; the old default only looked correct because single-line joining was hardcoded to `", "`.
- `Char` now renders as `'c'` rather than bare `c`.
- `java.util.HashMap` now renders as `[...]` like every other map, rather than `{...}`.
- The `AutoToString` members `p` and `c` were renamed to `describoPrintable` and
  `describoConfiguration` and made `protected`.

## Design notes

- `Configuration` is a flat case class with default arguments, which is the single, narrow, reviewed
  exception to this repository's `noDefaultArgs` guidance. It is an options DTO consumed with named
  arguments — the one shape where booleans are self-documenting at the call site — and a sixteen-field
  record without defaults would be unusable. It is also duplicated field-for-field in
  `com.worxbend.dscrbo`, which cannot depend on this module; any ADT would have to be duplicated too.
  **Any change to `Configuration` must be mirrored in dscrbo and in both READMEs.**
- The genuine decisions are modelled as ADTs, privately: `FieldRule` (`Omit` / `Redact` / `Render`)
  and `Layout` (`SingleLine` / `Multiline`). The `<= 0` sentinel of
  `multilineIfFieldsAreGreaterOrEqual` is laundered into a `Layout` at one place rather than being
  reinterpreted in scattered conditionals.
- Annotation scanning happens once per typeclass instance, at derivation time, and is materialised
  as a `Vector` — not a lazy `View` whose filter re-runs on every `size` and every `map`.
- Byte-for-byte parity with `dscrbo` is a test, not a promise. `PrintableParitySuite` here and
  `DescribeParitySuite` there declare the same fourteen-field `TestedType` with the same values and
  assert the same three strings under the same three configurations. Changing one module's output
  without changing the other's breaks both suites, which is the point — keep the fixture name, the
  fields, the values and the expected strings identical, or the comparison silently stops comparing.
- The build enables `-Wunused:all -deprecation -feature` and mixes in Mill's `ScalafmtModule`, the
  same as `dscrbo`, so dead code and hand-formatting are caught by `./mill libs.commons.describo.compile`
  and `./mill libs.commons.describo.checkFormat` rather than by review.
