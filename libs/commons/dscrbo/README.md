<div align="center">

# 🩻 dscrbo

**Configurable, redaction-aware `toString` for Scala 3 — with _zero_ runtime dependencies.**

[![Scala](https://img.shields.io/badge/Scala-3.8.4-DC322F?logo=scala&logoColor=white)](https://www.scala-lang.org)
[![Dependencies](https://img.shields.io/badge/runtime%20deps-0-success)](#-why-dscrbo)
[![Derivation](https://img.shields.io/badge/derivation-inline%20macro-blueviolet)](#-how-the-macro-works)
[![Tests](https://img.shields.io/badge/tests-244-brightgreen)](#-testing-and-the-conformance-kit)
[![License](https://img.shields.io/badge/license-MIT-blue)](../../../LICENSE)

_Print your case classes the way you want — and never print a secret by accident._

</div>

---

## 📑 Contents

- [Why dscrbo](#-why-dscrbo)
- [Install](#-install)
- [Quick start](#-quick-start)
- [Redaction and exclusion](#-redaction-and-exclusion)
- [Configuration](#️-configuration)
- [Output reference](#-output-reference)
- [Ways to derive](#-ways-to-derive)
- [How the macro works](#-how-the-macro-works)
- [Limits](#-limits)
- [dscrbo vs describo](#-dscrbo-vs-describo)
- [Testing and the conformance kit](#-testing-and-the-conformance-kit)
- [License](#-license)

---

## 💡 Why dscrbo

Scala's generated `toString` has two problems in a real service: it prints everything — including the
password field — and it prints it in exactly one shape you cannot change.

`dscrbo` fixes both, and does it **without putting anything on your classpath**.

| | |
| :-- | :-- |
| 🔐 **Secrets stay secret** | `@Redacted` and `@Excluded` fields are never dereferenced, and redaction **composes through nesting** — a secret three layers down inside a `Map` inside an `Option` is still redacted |
| 📦 **Zero runtime dependencies** | Nothing but the Scala standard library and the JDK. Nothing to shade, nothing to conflict, nothing in your dependency report |
| ⚡ **Everything decided at compile time** | Which fields are skipped, which are redacted, how each type is spelled — all resolved during macro expansion. No reflection, no `Mirror` walk, no per-call map building |
| 🎨 **16 formatting knobs** | Field names, type names, separators, affixes, qualified names, multiline layout |
| 🧬 **Handles real shapes** | Nested products, value classes, case objects, sealed families, enums — including generic ones like `Either` and `Try` |
| 🛡️ **Fails closed** | A field whose declared type is too vague to be sure about is a **compile error**, not a silently leaked `toString` |

> [!IMPORTANT]
> The zero-dependency property is this module's entire reason to exist. If you already depend on
> [magnolia](https://github.com/softwaremill/magnolia), prefer the sibling module
> [`describo`](../describo) — it is smaller, composes as an ordinary typeclass, and handles a few shapes
> `dscrbo` cannot. See [dscrbo vs describo](#-dscrbo-vs-describo).

---

## 📦 Install

| | |
| :-- | :-- |
| **groupId** | `io.worxbend` |
| **artifactId** | `dscrbo_3` |
| **Scala package** | `com.worxbend.dscrbo` |
| **Scala version** | 3.8.4 |

**Mill**

```scala
def mvnDeps = super.mvnDeps() ++ Seq(mvn"io.worxbend::dscrbo:0.1.0-SNAPSHOT")
```

**sbt**

```scala
libraryDependencies += "io.worxbend" %% "dscrbo" % "0.1.0-SNAPSHOT"
```

> [!NOTE]
> The groupId is `io.worxbend` while the Scala package is `com.worxbend.dscrbo`. That is intentional and
> not a mistake to be "fixed": `io.worxbend` is the established publishing organisation, and
> `com.worxbend.<product>` is the mandated package prefix for new code. The sibling publishes as
> `describo_3` under `com.worxbend.describo` — the leaf segments differ deliberately so both artifacts
> can share a classpath without their `Configuration` and `annotations` classes colliding.

---

## 🚀 Quick start

```scala
import com.worxbend.dscrbo.*
import com.worxbend.dscrbo.annotations.*

final case class Account(
    id:                 Long,
    email:              String,
    @Redacted password: String,
    @Excluded internal: String,
    roles:              List[String],
) derives Describe

given Configuration = Configuration()

Account(7L, "ada@example.com", "hunter2", "scratch", List("admin")).asString
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

---

## 🔐 Redaction and exclusion

| Annotation | Effect | Value read? |
| :-- | :-- | :-- |
| `@Redacted` | renders a replacement — `<redacted>` by default | ❌ never |
| `@Redacted("***")` | renders your replacement instead | ❌ never |
| `@Excluded` | the field disappears entirely | ❌ never |
| `@transient` | exact alias of `@Excluded`, kept for compatibility | ❌ never |

### Precedence — exclusion always wins 🥇

Resolved once per field at expansion time, first match wins:

1. `@Excluded` **or** `@transient` → the field is **omitted**
2. `@Redacted` → the field renders its **replacement**
3. otherwise → the field renders **normally**

```scala
final case class T(@Redacted @Excluded both: String, tag: String) derives Describe
// T(tag = "t")   — omitted, not redacted. Source order of the two annotations is irrelevant.
```

When a field carries several `@Redacted`, the one written **first** wins.

> [!TIP]
> `@Excluded` is the intended spelling. `@transient` is a *serialization* marker that `dscrbo` honours
> only so that existing code keeps working.

### Redaction composes 🪆

This is the property a hand-written `toString` loses, and the reason this library exists:

```scala
final case class Inner(@Redacted secret: String) derives Describe
final case class Outer(inner: Inner, xs: List[Inner], o: Option[Inner]) derives Describe

Outer(Inner("s"), List(Inner("s")), Some(Inner("s"))).asString
// Outer(inner = Inner(secret = <redacted>), xs = [Inner(secret = <redacted>)], o = Some(Inner(secret = <redacted>)))
```

---

## ⚙️ Configuration

> [!NOTE]
> This section is the practical reference. The **normative** specification shared with `describo` —
> and the one the conformance kit enforces — lives in
> [`docs/libraries/tostring-rendering-spec.md`](../../../docs/libraries/tostring-rendering-spec.md).
> If the two ever disagree, that page wins and this one is a bug.

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

The layout goes multiline when `multiline` is set **or** when the number of **rendered** fields reaches
`multilineIfFieldsAreGreaterOrEqual`. Excluded and transient fields do not count.

> [!WARNING]
> Any value `<= 0` **disables** the threshold — it does not mean "always multiline". Use
> `multiline = true` for that. `Configuration(multilineIfFieldsAreGreaterOrEqual = -1)` is the idiomatic
> way to force a single line.

### ✂️ fieldsSeparator

Used **verbatim** on a single line, so `" | "` really produces `a = 1 | b = 2`. In the multiline layout
its trailing whitespace is stripped before the newline, so `", "` yields `",\n"` and not a trailing
space at the end of every line.

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
| `java.util.*` collections and maps | identical to their Scala counterparts |
| nested case class | inline, through the same rules |
| case object / singleton enum case | its bare name |
| value class | unwrapped to its payload |
| `null` (anything) | `null` |

### Escaping

`\`, `"`, `\n`, `\t` and `\r` are escaped inside strings — backslash first — so a value containing a
comma or a parenthesis can never be confused with structure.

```scala
StringHolder("a,b)c").asString   // StringHolder(s = "a,b)c")
```

### `null` 🕳️

A `null` renders as the bare word `null` in **every** position — field, `Option` payload, collection
element, map key, map value. Never `"null"`, never `None`, never an exception. A `null` collection is
`null`, not `[]`; a `null` `Option` is `null`, not `None`.

A `null` in a `@Redacted` field still renders the replacement, because the value is never read.

### Type names 🏷️

Under `useTypeNames`, the **declared** type is printed — resolved at compile time from the field's
`TypeRepr`, never from `getClass`:

```text
roles: List = ["admin"]        ✅   not  roles: $colon$colon
meta:  Map  = ["k" -> "v"]     ✅   not  meta: Map1
opt:   Option = Some("v")      ✅   not  opt: Some
```

---

## 🧬 Ways to derive

### `derives Describe` — recommended

```scala
final case class Order(id: Long, total: BigDecimal) derives Describe

Order(1L, BigDecimal("9.99")).asString
```

### The `asString` extension

Picks up an ambient `Configuration`, falling back to `Configuration.default`:

```scala
given Configuration = Configuration(multiline = true)
order.asString

// or pass one explicitly
summon[Describe[Order]].describe(order)(using Configuration(useTypeNames = true))
```

### `AutoToString` — replace `toString` itself

```scala
final case class Order(id: Long) extends AutoToString derives Describe
object Order:
  given Configuration = Configuration()

println(Order(1L))   // Order(id = 1)
```

The mixin's members are prefixed (`dscrboDescribe`, `dscrboConfiguration`) precisely so they can never
collide with your own field names — a case class with fields called `p` and `c` still compiles.

> [!NOTE]
> `AutoToString` resolves its `Configuration` at the **class definition site**, so such a type always
> renders with one fixed configuration.

### `ToString.derived` — the low-ceremony shim

```scala
final case class Order(id: Long):
  override def toString: String = ToString.derived(this)
```

It renders in place without materialising an instance. Prefer `derives Describe` in new code: an
instance composes, a rendered string does not.

### Bring your own instance 🔧

A user-supplied `given` always wins over structural inlining — including for collection and `Option`
types:

```scala
given Describe[Money] with
  def describe(m: Money)(using Configuration): String = s"${m.amount} ${m.currency}"
```

---

## 🧠 How the macro works

Everything decidable from declared types is decided **once, at expansion time**: which fields are
omitted, which are redacted and with what replacement, how each type is spelled, which renderer each
field needs. What survives into your bytecode is straight-line string assembly plus the `Configuration`
reads that genuinely cannot be resolved earlier.

There is no `productElementNames` walk, no per-call `Map`, and no reflection.

**Resolution order for a field's type:**

1. a user-supplied `given Describe[T]` in scope 🥇
2. a built-in shape — `String`, `Char`, primitives, `Option`, collections, `Map`, `Array`, `java.util.*`
3. a case class, value class or case object → inlined structurally
4. a sealed family or enum → a dispatch over its children
5. an abstractly-typed field → **compile error** (see below)
6. any other concrete final class → its own `toString`

### 🛡️ Fail closed

A field whose declared type is a trait, an abstract class, an abstract type member, or `Any` **is a
compile error**. Such a value could at runtime be a case class with a `@Redacted` field, and rendering
it via `toString` would print that secret in the clear. The macro refuses rather than leak, and the
error names the type and tells you how to fix it:

```scala
given Describe[ThatType] = ...   // teach it the type, or
@Excluded thatField: ThatType    // exclude the field — its value is then never read
```

---

## 🚧 Limits

| Limit | Detail |
| :-- | :-- |
| 🔁 **Nesting: 12 types** | case classes, value classes and sealed families below the root. Wrappers don't count |
| 📚 **Nesting: 20 layers** | every layer of emitted code, wrappers included. Keeps the compiler's staging stack from overflowing |
| 🧊 **Generic case classes** | `Box[A] derives Describe` compiles but cannot be summoned at `Box[Int]` — see [below](#-dscrbo-vs-describo) |
| 📏 **64 KB class limit** | a model both very wide and very deep can exceed the JVM class-size limit. Inherent to unrolled inlining |
| 📐 **Nested multiline isn't re-indented** | a nested value is inserted verbatim, so its closing paren sits at the outer indent |

Both nesting caps produce a **refusal with a remedy**, never a `StackOverflowError` in your build. The
layer cap is a measured floor, not a derivation; the procedure to re-derive it is documented next to the
constant in `DescribeMacro.scala`.

---

## 🔀 dscrbo vs describo

Two implementations of **one specification**. For the same input and an equivalent `Configuration` they
produce **byte-identical** output — enforced by a shared conformance kit, not by convention.

| | 🩻 dscrbo | 🔎 [describo](../describo) |
| :-- | :-- | :-- |
| Mechanism | inline macro, fully unrolled | magnolia typeclass derivation |
| Runtime dependencies | **none** | magnolia |
| Type coverage | open — any concrete class renders | closed — needs an instance per type |
| Generic case classes | ❌ | ✅ |
| Extension point | `given Describe[T]` | `given Printable[T]` or a factory |
| Failure at the edges | compile error at 12 types / 20 layers | `StackOverflowError` at render time |

**Choose `dscrbo`** if you cannot take the magnolia dependency.
**Choose `describo`** otherwise — it is smaller and easier to extend.

### Known divergences

Two, both pinned by `KnownDivergenceSuite` in each module so neither can quietly become three:

1. **Enum case qualified names.** `dscrbo` prints `com.example.Colour.Red`; `describo` prints
   `com.example.Red`, because that is what magnolia's `TypeInfo` reports. Simple names agree — only
   `fullyQualifiedClassName` is affected.
2. **Generic case classes.** `describo` derives `Box[Int]` from `Box[A] derives Printable`. `dscrbo`
   cannot: its synthesised `derived$Describe[A]` needs a `Describe[A]`, and this module ships no
   per-type instances by design.

---

## ✅ Testing and the conformance kit

```bash
./mill libs.commons.dscrbo.test        # 244 tests
./mill libs.commons.__.test            # both modules
./mill libs.commons.__.checkFormat     # scalafmt
```

Parity with `describo` is enforced by `libs/commons/describo-tck`, a test-only module holding the
specification **as data** — a catalogue of `(fixture, configuration, expected string)` obligations that
both renderers must satisfy exactly. Each module supplies a thin adapter; neither *main* module depends
on the kit, so the zero-dependency guarantee is untouched.

Adding a case to the catalogue breaks every renderer that doesn't satisfy it. That is the point: before
the kit existed, the two engines silently disagreed on repeated `@Redacted` while both test suites
stayed green.

---

## 📄 License

MIT. See [LICENSE](../../../LICENSE).
