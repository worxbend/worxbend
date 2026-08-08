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
- [Testing](#-testing)
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

1. the instance being derived, if the field is the root type itself
2. a user-supplied `given Describe[T]` in scope 🥇
3. a built-in shape — `String`, `Char`, primitives, `Option`, collections, `Map`, `Array`, `java.util.*`
4. a sealed family, enum, case object or value class → **expanded structurally**
5. an abstractly-typed field → **compile error** (see below)
6. **a nested case class → its own `toString`** (see below)
7. any other concrete class → its own `toString`

### 🪆 Nested case classes are not unrolled

This is the design decision that shapes everything else. A case class is expanded structurally in exactly
two positions: **at the root**, where you wrote `derives Describe`, and **as a branch of a sealed family**,
which has no instance of its own to delegate to.

Anywhere else, a case-class-typed field is an ordinary typeclass dependency. It earns structured rendering
by carrying its own `derives Describe`; without one it renders the way Scala already renders it.

**Tuples are the exception**, because they are anonymous containers rather than domain types — you cannot
write `derives Describe` on `Tuple2`, so delegating would strip their structure permanently *and* bypass the
instances of the elements inside them. Tuples are expanded wherever they appear, like the collections they
resemble, and their elements go back through the normal resolution:

```scala
final case class Holder(p: (Int, Inner)) derives Describe
// Holder(p = Tuple2(_1 = 1, _2 = Inner(a = 1)))    <- Inner's own instance still applies
```

Collections follow the same principle: the container is always structural, and each **element** is resolved
independently, so `List[Inner]` renders `[Inner(a = 1)]` when `Inner` has an instance and `[Inner(1)]` when it
does not.

```scala
final case class Inner(a: Int, s: String)                    // no instance
final case class Outer(i: Inner) derives Describe
Outer(Inner(1, "v")).asString                                // Outer(i = Inner(1,v))     <- Inner's own toString

final case class Inner(a: Int, s: String) derives Describe   // has one
Outer(Inner(1, "v")).asString                                // Outer(i = Inner(a = 1, s = "v"))
```

Why: unrolling made one derivation's emitted expression grow with the entire reachable object graph. That
is what forced the two depth caps, a cycle detector, and exposure to the JVM's per-method bytecode limit —
and delegating measured **1.51× faster and 13.5× smaller** at depth 12. Enums, sealed families and value
classes keep their structural treatment, because for those inlining is the whole point.

> [!WARNING]
> One exception, and it is a compile error rather than a surprise. A nested case class that declares
> `@Redacted` or `@Excluded` but has **no instance** is refused: delegating *that* to `toString` would print
> exactly what the annotation exists to hide. The error names the field and tells you to add
> `derives Describe` to it.

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
| 🔁 **Nesting: 12 types** | only the shapes that still expand — sealed families, value classes, wrapper chains. Plain nested case classes do not nest at all |
| 📚 **Nesting: 20 layers** | every layer of emitted code, wrappers included. Binds first: a sealed chain refuses at 11 levels |
| 🧊 **Generic case classes** | `Box[A] derives Describe` compiles, but summoning it at `Box[Int]` needs a `given Describe[Int]` in scope. This module ships no per-type instances, so it fails out of the box for built-in element types and works as soon as you supply one — see [below](#-dscrbo-vs-describo) |
| 📏 **Per-method bytecode limit** | the JVM's 65,535-byte `Code` attribute, per method. Far harder to reach now that nesting delegates, but a single very wide product can still approach it |
| 📐 **Nested multiline isn't re-indented** | a nested value is inserted verbatim, so its closing paren sits at the outer indent |

Both nesting caps produce a **refusal with a remedy**, never a `StackOverflowError` in your build. The
layer cap is a measured floor, not a derivation; the procedure to re-derive it is documented next to the
constant in `DescribeMacro.scala`.

---

## 🔀 dscrbo vs describo

Two libraries solving the same problem by different means. **They are not required to produce identical
output**, and they deliberately do not: each is free to make the choice that suits its own mechanism, and
each pins its own behaviour in its own tests.

| | 🩻 dscrbo | 🔎 [describo](../describo) |
| :-- | :-- | :-- |
| Mechanism | inline macro, fully unrolled | magnolia typeclass derivation |
| Runtime dependencies | **none** | magnolia |
| Type coverage | open — any concrete class renders | closed — needs an instance per type |
| Nested case classes | delegate to their own instance, else `toString` | auto-derived by magnolia |
| Generic case classes | only with a `given` for the type argument | ✅ |
| Extension point | `given Describe[T]` | `given Printable[T]` or a factory |
| Failure at the edges | compile error at 12 types / 20 layers | `StackOverflowError` at render time |

**Choose `dscrbo`** if you cannot take the magnolia dependency.
**Choose `describo`** otherwise — it is smaller and easier to extend.

### Where they differ

| | `dscrbo` | `describo` |
| :-- | :-- | :-- |
| Nested case class, no instance | plain `toString` | auto-derived by magnolia |
| Enum case, qualified name | `com.example.Colour.Red` | `com.example.Red` — magnolia's `TypeInfo` reports the package |
| Generic case class | needs a `given` for the type argument | derives unaided |

The first is the design of this module rather than a shortfall: nested types are not unrolled into their
parent. The other two follow from the derivation mechanism each library uses.

## ✅ Testing

```bash
./mill libs.commons.dscrbo.test        # this module
./mill libs.commons.__.checkFormat     # scalafmt
```

Every test lives in this module. There is no shared conformance suite and no cross-library contract to
satisfy: the two renderers are independent, so each one's behaviour is pinned where that behaviour is
implemented.

---

## 📄 License

MIT. See [LICENSE](../../../LICENSE).
