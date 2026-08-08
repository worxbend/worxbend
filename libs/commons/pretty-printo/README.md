<div align="center">

# 🔎 pretty-printo

**Configurable, redaction-aware `toString` derivation for Scala 3, built on [Magnolia](https://github.com/softwaremill/magnolia).**

[![Scala](https://img.shields.io/badge/Scala-3.8.4-DC322F?logo=scala&logoColor=white)](https://www.scala-lang.org)
[![Derivation](https://img.shields.io/badge/derivation-magnolia-8A2BE2)](https://github.com/softwaremill/magnolia)
[![Tests](https://img.shields.io/badge/tests-217-brightgreen)](#-testing)
[![License](https://img.shields.io/badge/license-MIT-blue)](../../../LICENSE)

_You describe **what** a value should look like; pretty-printo works out **how** from the type's structure._

</div>

---

## 📑 Contents

- [Why pretty-printo](#-why-pretty-printo)
- [What's with the name?](#-whats-with-the-name)
- [Install](#-install)
- [Quick start](#-quick-start)
- [Redaction and exclusion](#-redaction-and-exclusion)
- [Configuration](#️-configuration)
- [Output reference](#-output-reference)
- [Supported types](#-supported-types)
- [Writing your own instance](#-writing-your-own-instance)
- [Value classes](#-value-classes)
- [Limits](#-limits)
- [pretty-printo vs reveal](#-pretty-printo-vs-reveal)
- [Testing](#-testing)
- [Compatibility](#-compatibility)
- [Design notes](#-design-notes)
- [License](#-license)

---

## 💡 Why pretty-printo

Scala's generated `toString` has two problems in a real service: it prints **everything** — including
the password field — and it prints it in exactly **one shape** you cannot change.

| | |
| :-- | :-- |
| 🔐 **Secrets stay secret** | `@Redacted` and `@Excluded` fields are never dereferenced, and redaction **composes through nesting** — a secret inside a `Map` inside an `Option` is still redacted |
| 🧬 **Ordinary typeclass** | `PrettyPrintable[A]` composes like any other. Give a third-party type an instance and every model that reaches it renders properly |
| 🪆 **Auto-derivation** | Nested case classes are derived implicitly; you write `derives PrettyPrintable` once at the top |
| 🎨 **16 formatting knobs** | Field names, type names, separators, affixes, qualified names, multiline layout |
| 🛡️ **No reflection at render time** | Type names come from the typeclass, not from `getClass`, so a `null` or redacted field is never touched |
| 🧩 **Generic case classes** | `Box[A] derives PrettyPrintable` derives at the instantiated type with no ceremony |

> [!TIP]
> If you **cannot take the magnolia dependency**, the sibling module [`reveal`](../reveal) solves the
> same problem with a hand-written inline macro and *zero* runtime dependencies. The two are
> independent — see [pretty-printo vs reveal](#-pretty-printo-vs-reveal).

---

## 🧝 What's with the name?

In *Disenchantment*, every elf out of Elfwood is named for the one thing they do, with an `-o` stapled
on the end. Elfo is an elf. Sorcerio does sorcery. Shocko, Speako, Weirdo, Leavo — the joke is that the
name is the job description and nobody tried very hard.

So: this library describes things. **pretty-printo.**

Its sibling, [`reveal`](../reveal), is not an elf name at all — that one says what it does without the
costume.

---

## 📦 Install

| | |
| :-- | :-- |
| **groupId** | `com.worxbend` |
| **artifactId** | `pretty-printo_3` |
| **Scala package** | `com.worxbend.prettyprinto` |
| **Scala version** | 3.8.4 |
| **Dependency** | `com.softwaremill.magnolia1_3::magnolia` |

**Mill**

```scala
def mvnDeps = super.mvnDeps() ++ Seq(mvn"com.worxbend::pretty-printo:0.1.0-SNAPSHOT")
```

**sbt**

```scala
libraryDependencies += "com.worxbend" %% "pretty-printo" % "0.1.0-SNAPSHOT"
```

---

## 🚀 Quick start

```scala
import com.worxbend.prettyprinto.*
import com.worxbend.prettyprinto.annotations.*

final case class Account(
    id:                 Long,
    email:              String,
    @Redacted password: String,
    @Excluded internal: String,
    roles:              List[String],
) derives PrettyPrintable

summon[PrettyPrintable[Account]].asString(
  Account(7L, "ada@example.com", "hunter2", "scratch", List("admin"))
)
```

```text
Account(
  id = 7,
  email = "ada@example.com",
  password = <redacted>,
  roles = ["admin"]
)
```

The password is replaced, the internal field is gone, and neither value was ever read. 🎉

### The `AutoToString` mixin

```scala
given Configuration = Configuration(multiline = true)

final case class Account(@Redacted password: String, name: String)
    extends AutoToString derives PrettyPrintable

println(Account("hunter2", "Ada"))
```

> [!NOTE]
> The mixin's `Configuration` is `scala.compiletime.deferred`, so it is resolved **once, at the
> class-definition site** — a type that mixes it in renders with one fixed configuration for life. To
> vary configuration per call, use the typeclass directly.
>
> Both members are named `prettyPrintoInstance` and `prettyPrintoConfiguration` and are `protected`. They
> share a namespace with your fields, so short names are not safe: an earlier version declared
> `given p` and `given c`, and any case class with a field named `p` or `c` failed to compile.

---

## 🔐 Redaction and exclusion

| Annotation | Effect | Value read? |
| :-- | :-- | :-- |
| `@Redacted` | renders a replacement — `<redacted>` by default | ❌ never |
| `@Redacted("***")` | renders your replacement instead | ❌ never |
| `@Excluded` | the field disappears entirely | ❌ never |
| `@transient` | alias of `@Excluded`, kept for compatibility | ❌ never |

### Precedence — exclusion always wins 🥇

Resolved once per field, first match wins:

1. `@Excluded` **or** `@transient` → the field is **omitted**
2. `@Redacted` → the field renders its **replacement**
3. otherwise → the field renders **normally**

So `@Redacted @Excluded both: String` is omitted, not redacted, and the source order of the two
annotations is irrelevant.

When a field carries several `@Redacted`, **the one written first in source order wins**. Magnolia
surfaces `param.annotations` in reverse source order, so `FieldRule.of` reverses before searching.

> [!TIP]
> `@transient` is a *serialization* marker. pretty-printo honours it only because earlier versions did;
> `@Excluded` is the intended spelling for new code.

### Redaction composes 🪆

```scala
final case class Inner(@Redacted secret: String) derives PrettyPrintable
final case class Outer(inner: Inner, xs: List[Inner], o: Option[Inner]) derives PrettyPrintable

// Outer(inner = Inner(secret = <redacted>), xs = [Inner(secret = <redacted>)], o = Some(Inner(secret = <redacted>)))
```

---

## ⚙️ Configuration

Every field renders as:

```text
fieldNamePrefix name fieldNameSuffix
  [ fieldNameAndTypeNameSeparator typeNamePrefix Type typeNameSuffix ]
  fieldNameAndValueSeparator valuePrefix value valueSuffix
```

The bracketed group appears only under `useTypeNames`; the whole name group is dropped when
`useFieldNames` is off.

| Option | Default | What it does |
| :-- | :-- | :-- |
| `useFieldNames` | `true` | render `name = value` instead of a bare `value` |
| `useTypeNames` | `false` | render each field's **declared** type |
| `fullyQualifiedClassName` | `false` | qualified names instead of simple ones |
| `shortPackagePrefix` | `true` | compress `com.worxbend.example.Order` → `c.w.e.Order` |
| `fieldsSeparator` | `", "` | between fields; used verbatim on one line |
| `fieldNamePrefix` | `""` | before each field name |
| `fieldNameSuffix` | `""` | after each field name |
| `fieldNameAndValueSeparator` | `" = "` | between the name group and the value |
| `fieldNameAndTypeNameSeparator` | `": "` | between name and type |
| `typeNamePrefix` | `""` | before each type name |
| `typeNameSuffix` | `""` | after each type name |
| `valuePrefix` | `""` | before every value |
| `valueSuffix` | `""` | after every value |
| `multiline` | `false` | force one field per line |
| `multilineIndent` | `"  "` | per-field indent when multiline |
| `multilineIfFieldsAreGreaterOrEqual` | `5` | switch to multiline at this many fields |

### 🔢 The multiline threshold and its sentinel

The layout goes multiline when `multiline` is set **or** when the number of **rendered** fields
reaches `multilineIfFieldsAreGreaterOrEqual`. Excluded and transient fields do not count.

> [!WARNING]
> Any value `<= 0` **disables** the threshold — it does not mean "always multiline". Use
> `multiline = true` for that. `Configuration(multilineIfFieldsAreGreaterOrEqual = -1)` is the
> idiomatic way to force a single line.

### ✂️ fieldsSeparator

Used **verbatim** on a single line, so `" | "` really produces `a = 1 | b = 2`. In multiline its
trailing whitespace is stripped before the newline, so `", "` yields `",\n"` rather than a trailing
space on every line.

---

## 🎨 Output reference

| Shape | Renders as |
| :-- | :-- |
| `String` | `"quoted"` |
| `Char` | `'q'` |
| numbers, `Boolean` | bare — `1`, `2.5`, `true` |
| `BigDecimal` | keeps its scale — `1000.50` |
| `List` / `Vector` / `Set` / `Seq` / `Array` | `[a, b]` |
| `Map` | `["k" -> "v"]` |
| `Option` | `Some(x)` / `None` |
| tuple | `Tuple2(_1 = 1, _2 = "a")` — an ordinary product to Magnolia |
| `java.util.*` collections and maps | identical to their Scala counterparts |
| nested case class | inline, through the same rules |
| case object / parameterless enum case | its bare name |
| value class | unwrapped to its payload |
| `null` (anything) | `null` |

### Escaping

`\`, `"`, `\n`, `\t` and `\r` are escaped inside strings — backslash first — at **every** level, so a
value containing a comma or a parenthesis can never be confused with structure.

### `null` 🕳️

Renders as the bare word `null` in **every** position — field, `Option` payload, collection element,
map key, map value. Never `"null"`, never `None`, never an exception. A `null` collection is `null`,
not `[]`; a `null` `Option` is `null`, not `None`. A `null` in a `@Redacted` field still renders the
replacement, because the value is never read.

### Type names 🏷️

Under `useTypeNames` the **declared** type is printed, carried on the typeclass rather than read from
`getClass`:

```text
roles: List   = ["admin"]      ✅   not  roles: $colon$colon
meta:  Map    = ["k" -> "v"]   ✅   not  meta: Map1
opt:   Option = Some("v")      ✅   not  opt: Some
count: Int    = 1              ✅   not  count: Integer
```

That is also what makes redaction safe: rendering a field's type never requires dereferencing it.

---

## 🧱 Supported types

Instances ship for `String`, `Char`, `Int`, `Long`, `Short`, `Byte`, `Double`, `Float`, `Boolean`,
`BigInt`, `BigDecimal`, `java.lang.Integer`, `java.lang.Character`, `java.time.{LocalDate, LocalTime,
Instant, Duration, Period, ZonedDateTime, OffsetDateTime, OffsetTime}`, `Option`, `Iterable`, `Seq`,
`IndexedSeq`, `List`, `Vector`, `Set`, `Array`, `Map`, and `java.util.{List, ArrayList, LinkedList,
Set, HashSet, Map, HashMap}`.

> [!IMPORTANT]
> The typeclass is **invariant**. That is what guarantees a field declared `List[String]` picks the
> `List` instance and not the `Iterable` one — but it also means only the *exact declared type*
> resolves. A field typed `ArrayDeque`, `TreeMap` or `UUID` needs its own instance.
>
> This is a **closed** type set, which is the main ergonomic difference from `reveal`. For example
> `scala.util.Try` does not compile out of the box, because `Failure` reaches `Throwable` and nothing
> supplies an instance for it — one line fixes that, and `PrettyPrintableCompositionSuite` pins both halves.

---

## 🔧 Writing your own instance

`PrettyPrintable` has two members — `printedType` and the `asString` extension — but you rarely implement
them by hand. Four factories fill both in and give you this module's escaping, `null` handling and
element separator for free. Each takes the simple and the fully qualified name, which is what
`useTypeNames` and `fullyQualifiedClassName` print.

```scala
import com.worxbend.prettyprinto.*
import scala.jdk.CollectionConverters.*

// A scalar rendered by a plain function. The string is used verbatim, so quote it yourself if the
// type should appear quoted.
given PrettyPrintable[java.util.UUID] =
  PrettyPrintable.instance("UUID", "java.util.UUID")(_.toString)

// A container rendered as [a, b, c]; elements go through their own instances.
given PrettyPrintable[java.util.ArrayDeque[String]] =
  PrettyPrintable.collection("ArrayDeque", "java.util.ArrayDeque")(_.asScala.iterator)

// A container rendered as [k -> v]; keys and values go through their own instances.
given PrettyPrintable[java.util.TreeMap[String, Int]] =
  PrettyPrintable.mapping("TreeMap", "java.util.TreeMap")(_.asScala.iterator)

// A wrapper rendered as its payload but named after the wrapper.
given PrettyPrintable[UserId] =
  PrettyPrintable.valueClass("UserId", "com.example.UserId")(_.value)
```

`PrettyPrintableFactorySuite` is exactly the code above, so the snippet cannot rot.

---

## 💎 Value classes

Scala 3 synthesises no `Mirror` for a value class, so **`derives PrettyPrintable` cannot be used on one** —
Magnolia never sees it. Give it an instance explicitly:

```scala
final case class UserId(value: String) extends AnyVal
object UserId:
  given PrettyPrintable[UserId] = PrettyPrintable.valueClass("UserId", "com.example.UserId")(_.value)
```

The wrapper renders as its payload (`"u1"`) but is *named* after the wrapper, so a `UserId` field
prints `id: UserId = "u1"` under `useTypeNames`. `@Redacted` on a *field* of value-class type is
honoured without unwrapping.

> [!WARNING]
> Because the instance reads the payload directly, nothing inspects the value class's **own
> parameter**: an `@Excluded`, `@transient` or `@Redacted` written there has **no effect**.
> Annotations belong on the fields of the enclosing case class.

---

## 🚧 Limits

| Limit | Detail |
| :-- | :-- |
| 📐 **Nested multiline isn't re-indented** | A nested case class is inserted verbatim, so its lines are not indented relative to the parent. Use a single-line configuration for deeply nested values |
| 🔁 **Depth is bounded by the stack** | Recursive descent, one JVM frame per level, so a value nested a few hundred levels deep throws `StackOverflowError` — roughly where the generated `toString` would. Breadth is unaffected; collections render iteratively. There is no configurable cap |
| 💎 **Value classes need an explicit instance** | See above. `PrettyPrintableStructureSuite` pins this with an `assertDoesNotCompile`, so the day Scala changes it a test fails rather than the behaviour drifting |
| 🏷️ **Enum cases under `fullyQualifiedClassName`** | Magnolia's `TypeInfo` reports the enclosing *package*, not the enclosing enum, so `Colour.Red` prints `com.example.Red`. Simple names are unaffected |
| 🔀 **Set and Map iteration order** | Insertion order only up to four elements; beyond that Scala switches to a hashed representation. Sort before rendering if you need stable output |

---

## 🔀 pretty-printo vs reveal

Two libraries solving the same problem by different means. **They are independent and are not
required to produce identical output** — each is free to make the choice that suits its mechanism,
and each pins its own behaviour in its own tests.

| | 🔎 pretty-printo | 🩻 [reveal](../reveal) |
| :-- | :-- | :-- |
| Mechanism | Magnolia typeclass derivation | inline macro, expanded at the use site |
| Runtime dependencies | magnolia | **none** |
| Type coverage | closed — an instance per type | open — any concrete class renders via `toString` |
| Nested case class, no instance | auto-derived | plain `toString`; never unrolled into the parent |
| Generic case class | ✅ derives unaided | needs a `given` for the type argument |
| Enum case, qualified name | `com.example.Red` | `com.example.Colour.Red` |
| Failure at the edges | `StackOverflowError` at render time | compile-time refusal at its nesting caps |

**Choose `pretty-printo`** when you already have magnolia, want typeclass composition, or need instances
for third-party types. **Choose `reveal`** when you cannot take the dependency.

---

## ✅ Testing

```bash
./mill libs.commons.pretty-printo.test          # 217 tests
./mill libs.commons.pretty-printo.checkFormat   # scalafmt
```

Every test lives in this module. There is no shared suite and no cross-library contract: the two
renderers are independent, so each one's behaviour is pinned where that behaviour is implemented.

---

## 🔄 Compatibility

Version `0.1.0-SNAPSHOT` has never been published, and the following source-breaking changes were
made together, on purpose, before the first release:

- The package moved to `com.worxbend.prettyprinto`, and the publishing organisation to
  `com.worxbend`, so groupId and package prefix now agree.
- `Configuration` and `annotations.{Redacted, Excluded}` moved out of `object PrettyPrintable` into the
  package.
- `PrettyPrintable.TypeAliases` was **deleted**. It mapped runtime class names such as `$colon$colon` and
  `Map2` back to friendly names; type names now come from the declared type, so there is nothing left
  to map.
- `PrettyPrintable` gained an abstract member, `printedType`. Hand-written instances must supply it — the
  four factories supply it for you.
- `Configuration.fieldsSeparator`'s default changed from `","` to `", "`. The field is now actually
  read; the old default only looked correct because single-line joining was hardcoded to `", "`.
- `Char` now renders as `'c'` rather than bare `c`.
- `java.util.HashMap` now renders as `[...]` like every other map, rather than `{...}`.
- The `AutoToString` members `p` and `c` were renamed to `prettyPrintoInstance` and
  `prettyPrintoConfiguration` and made `protected`.

---

## 🏗️ Design notes

- **`Configuration` is a flat case class with default arguments**, the single narrow exception to this
  repository's `noDefaultArgs` guidance. It is an options DTO consumed with named arguments — the one
  shape where booleans are self-documenting at the call site — and a sixteen-field record without
  defaults would be unusable. It is duplicated field-for-field in `com.worxbend.reveal`, which cannot
  depend on this module. **Any change must be mirrored there and in both READMEs.**
- **The genuine decisions are ADTs**, privately: `FieldRule` (`Omit` / `Redact` / `Render`) and
  `Layout` (`SingleLine` / `Multiline`). The `<= 0` sentinel is laundered into a `Layout` in one place
  rather than reinterpreted in scattered conditionals.
- **Annotation scanning happens once per typeclass instance**, at derivation time, materialised as a
  `Vector` — not a lazy `View` whose filter re-runs on every `size` and every `map`.
- **The build enables `-Wunused:all -deprecation -feature`** and mixes in Mill's `ScalafmtModule`, so
  dead code and hand-formatting are caught by the build rather than by review.

---

## 📄 License

MIT. See [LICENSE](../../../LICENSE).
