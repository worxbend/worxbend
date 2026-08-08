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

| Annotation                        | Effect                                                        |
| --------------------------------- | ------------------------------------------------------------- |
| `@Redacted`                       | Prints `<redacted>` instead of the value.                      |
| `@Redacted(replacement = "***")`  | Prints `***` instead of the value.                             |
| `@Excluded`                       | Omits the field entirely.                                      |
| `@transient`                      | Exact alias of `@Excluded`, kept for backwards compatibility.  |

Resolution is a single ordered decision per field, evaluated once when the instance is derived:

1. `@Excluded` **or** `@transient` → the field is omitted;
2. otherwise `@Redacted` → the replacement is printed;
3. otherwise → the value is rendered.

Consequences, all of them deliberate:

- **Exclusion beats redaction.** `@Redacted @Excluded both: String` disappears; it does not print a
  replacement. The source order of the two annotations is irrelevant — precedence is by rule, not
  by position.
- **An omitted or redacted field is never dereferenced.** Not for its value, not for its type name,
  not for a null check. A `null` `@Redacted` field prints its replacement instead of throwing.
- **The declared type is still printed** for a redacted field under `useTypeNames`, because the type
  comes from the typeclass instance rather than from the value.
- `@transient` is a *serialization* marker. describo honours it only because earlier versions did;
  `@Excluded` is the intended spelling for new code.
- Several `@Redacted` annotations on one field are not an error: **the one written first in source
  order wins**. Magnolia surfaces `param.annotations` in reverse source order, so `FieldRule.of`
  reverses before searching; without that, this module and `dscrbo` — whose macro sorts by
  `pos.start` and takes the head — would disagree on the same input. Deterministic, and identical in
  both modules.

## Configuration reference

`Configuration` is a flat, sixteen-field options record. Every field has a default, so you set only
what you care about with named arguments:

```scala
Configuration(multiline = true, useTypeNames = true)
```

| Field | Default | Meaning |
| ----- | ------- | ------- |
| `useFieldNames` | `true` | Render `field = value` rather than a bare `value`. Also suppresses type names when `false`. |
| `useTypeNames` | `false` | Render each field's declared type. |
| `fullyQualifiedClassName` | `false` | Use fully qualified names for the type and for field types. |
| `shortPackagePrefix` | `true` | With the above, compress leading lowercase segments: `c.w.d.Account`. |
| `fieldsSeparator` | `", "` | Inter-**field** separator. Used verbatim on one line; trailing-stripped in multiline. |
| `fieldNamePrefix` | `""` | Inserted before each field name. |
| `fieldNameSuffix` | `""` | Inserted after each field name. |
| `fieldNameAndValueSeparator` | `" = "` | Between the name (or type) and the value. |
| `fieldNameAndTypeNameSeparator` | `": "` | Between the field name and the type name. |
| `typeNamePrefix` | `""` | Inserted before each type name. |
| `typeNameSuffix` | `""` | Inserted after each type name. |
| `valuePrefix` | `""` | Inserted before each rendered value, `null` included. |
| `valueSuffix` | `""` | Inserted after each rendered value, `null` included. |
| `multiline` | `false` | Always render one field per line. |
| `multilineIndent` | `"  "` | Per-field indentation in multiline layout. |
| `multilineIfFieldsAreGreaterOrEqual` | `5` | Switch to multiline at this many rendered fields. **`<= 0` disables the threshold entirely.** |

### `multilineIfFieldsAreGreaterOrEqual`

This is the one non-obvious knob. The layout is chosen once per render:

```
Multiline  iff  multiline
             || (multilineIfFieldsAreGreaterOrEqual > 0
                 && renderedFieldCount >= multilineIfFieldsAreGreaterOrEqual)
```

- `renderedFieldCount` is counted **after** exclusion, so `@Excluded` fields do not push a type over
  the threshold.
- **Any value `<= 0` disables the threshold.** `0` and `-1` behave identically; neither means "always
  multiline". Set `multiline = true` for that.
- If nothing at all is rendered — an empty case class, or one whose fields are all excluded — the
  layout collapses to a single line and you get `T()`, even under `multiline = true`.

### `fieldsSeparator`

- **Single line:** fields are joined with the separator *verbatim*. `" | "` gives `a = 1 | b = 2`.
- **Multiline:** each field is prefixed with `multilineIndent` and joined with
  `fieldsSeparator.stripTrailing() + "\n"`. Only *trailing* whitespace is dropped, and only here,
  because it would otherwise be invisible whitespace at the end of every line. Leading whitespace
  survives, so `" | "` ends each line with ` |`.
- With no rendered fields the separator is not used at all.

The separator is an inter-**field** knob. Collection elements and map entries always use `", "`.

## Output format

| Shape | Rendering |
| ----- | --------- |
| String | `"text"`, escaped |
| Char | `'c'`, escaped |
| Numbers, `Boolean`, `java.time.*` | their own `toString`, unquoted |
| `null` | the four characters `null`, unquoted, in every position |
| `List`, `Vector`, `Set`, `Seq`, `IndexedSeq`, `Iterable`, `Array` | `["a", "b"]` |
| `java.util.List` / `ArrayList` / `LinkedList` / `Set` / `HashSet` | `["a", "b"]` |
| `Map`, `java.util.Map`, `java.util.HashMap` | `["key" -> "value"]` |
| `Option` | `Some("payload")` / `None` |
| Case class | `Name(field = value, ...)` |
| Empty or fully excluded case class | `Name()` |
| Case object, parameterless enum case | `Name`, no parentheses |
| Value class | its payload's rendering |

Escaping, applied at every position (top-level field, collection element, map key, map value,
`Option` payload), in this order: `\` → `\\`, `"` → `\"`, newline → `\n`, carriage return → `\r`,
tab → `\t`, and additionally `'` → `\'` inside a `Char`. Nothing else is escaped — no unicode
escapes, no control characters — so the two modules stay trivially identical.

### `null`

A `null` renders as `null` in every position: top-level field, `Option` payload, collection element,
map key, map value. Never `"null"`, never `None`, never `[]`, never an empty string, never an
exception. `valuePrefix`/`valueSuffix` still apply, so with `valuePrefix = "["` a null field renders
`[null]`. Nullness has no effect on the printed type: `name: String = null`.

### Type names

Under `useTypeNames`, describo prints the **declared** type, dealiased and widened, with type
arguments dropped: `Int`, `String`, `List`, `Map`, `Option`, `BigDecimal`, `LocalDate`. It never
prints a runtime class, so you will not see `Integer`, `Some`, `$colon$colon` or `Map2`. The type
comes from the typeclass instance (`Printable.printedType`), which is also why redacted, excluded
and null fields can be typed without being touched.

Fully qualified spellings follow `TypeRepr.of[X].dealias.typeSymbol.fullName`, for example
`scala.Int`, `java.lang.String`, `scala.collection.immutable.List`, `scala.math.BigDecimal`.
Package compression shortens every *leading* lowercase segment to one character and leaves the rest
alone: `com.worxbend.describo.Account` → `c.w.d.Account`, `Account` → `Account` (no leading dot),
`com.worxbend.describo.Fixtures.Inner` → `c.w.d.Fixtures.Inner`.

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
