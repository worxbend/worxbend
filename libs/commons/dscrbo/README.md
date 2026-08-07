# dscrbo

Configurable, redaction-aware `toString` for Scala 3, built on a hand-written inline macro with
**zero runtime dependencies**.

`dscrbo` renders case classes, case objects, sealed families — generic ones included — and enums as
strings you choose the shape of, and it never prints a field you marked as a secret: not at the top
level, not nested three layers down inside a `Map` inside an `Option`, and not through a field whose
declared type is too vague to be sure about, which is refused at compile time rather than rendered.

---

## Why this module exists

The whole point of `dscrbo` is the dependency count: **zero**. It depends on nothing but the Scala
standard library and the JDK. Nothing is added to your classpath, nothing shades, nothing conflicts.
scalatest is used in the test scope only.

Its sibling, `describo`, does the same job on top of [magnolia](https://github.com/softwaremill/magnolia).
If you already have magnolia, prefer `describo`: it is smaller and easier to extend with per-type
instances. If you cannot take the dependency, use this module. **The two are kept byte-identical in
output** for the same input and an equivalent `Configuration`, and both test suites contain the same
parity fixture with the same expected strings so that any drift is caught immediately.

### Coordinates and packages

| | |
| --- | --- |
| groupId | `io.worxbend` |
| artifactId | `dscrbo_3` |
| Scala package | `com.worxbend.dscrbo` |

The groupId is `io.worxbend` while the Scala package is `com.worxbend.dscrbo`. That is intentional and
not a mistake to be "fixed": `io.worxbend` is the established publishing organisation, and
`com.worxbend.<product>` is the mandated package prefix for new code. The sibling module publishes as
`describo_3` under package `com.worxbend.describo`; the leaf segments differ deliberately so that both
artifacts can sit on one classpath without their `Configuration` and `annotations` classes colliding.

---

## Usage

### `derives Describe` — the recommended form

```scala
import com.worxbend.dscrbo.Describe
import com.worxbend.dscrbo.annotations.Redacted

final case class User(name: String, @Redacted password: String) derives Describe

Describe[User].describe(User("ada", "hunter2"))
// User(name = "ada", password = <redacted>)
```

`derives Describe` puts an instance in the companion object. Instances **compose**: a field whose type
has its own instance is rendered by that instance, so redaction survives nesting.

### The `asString` extension

```scala
import com.worxbend.dscrbo.Configuration

given Configuration = Configuration(multiline = true)

User("ada", "hunter2").asString
```

`asString` is the same user-facing verb `describo` exposes. It picks up an ambient `Configuration`, or
falls back to `Configuration.default` through its default argument when implicit search finds nothing.

### `AutoToString` — replace `toString` itself

```scala
import com.worxbend.dscrbo.AutoToString

final case class Session(id: String, @Redacted token: String) extends AutoToString

println(Session("s-1", "t-1")) // Session(id = "s-1", token = <redacted>)
```

The mixin's members are called `dscrboDescribe` and `dscrboConfiguration` rather than something short.
That is deliberate: short names such as `p` and `c` collide with user field names of the same spelling.
A case class with fields named `p` and `c` compiles, and there is a regression test for it.

The mixin's `Configuration` is `deferred`, so a `given Configuration` must be in scope where the class
is **defined** — exactly as in `describo`. It is fixed there and cannot vary per call; anything that
needs to vary the configuration should call `Describe[T].describe(value)(using conf)` explicitly.

### `ToString.derived` — the low-ceremony shim

```scala
final case class Legacy(@Redacted secret: String):
  override def toString: String = ToString.derived(this)
```

This is the module's original entry point and is retained. It renders in place without materialising an
instance. Prefer `derives Describe` in new code: an instance composes into other types, a rendered
string does not.

---

## Annotations

```scala
import com.worxbend.dscrbo.annotations.Excluded
import com.worxbend.dscrbo.annotations.Redacted
```

| Annotation | Effect |
| --- | --- |
| `@Excluded` | The field is omitted entirely. Its value is never read. |
| `@transient` | Exact alias of `@Excluded`. |
| `@Redacted` | The field renders `<redacted>` in place of its value. The value is never read. |
| `@Redacted(replacement = "***")` | Same, with a custom replacement. Must be a string literal. |

### Precedence — exclusion beats redaction

Each field is resolved exactly once, at compile time, by one ordered function over one ADT. First match
wins:

1. `@Excluded` **or** `@transient` → the field is omitted;
2. otherwise `@Redacted` → the field renders its replacement;
3. otherwise → the field renders normally.

So `@Redacted @Excluded both: String` is **omitted**, not redacted. The order the two annotations are
written in is irrelevant: precedence is by rule, not by source position.

`@transient` is a serialization marker, honoured here only for backwards compatibility. `@Excluded` is
the intended spelling. Both map to the same rule in one place in the macro, which is what makes the
precedence above impossible to get wrong.

If a field carries more than one `@Redacted`, the first one in declaration order wins. This is
deterministic, not an error.

### The values really are never read

For an omitted or redacted field the macro emits **no accessor call at all** — not for the value, not
for a type name, not for a null check. Consequences:

- a `null` `@Redacted` field prints its replacement instead of throwing;
- a field whose type the macro could not otherwise render can still be redacted or excluded.

Under `useTypeNames` a redacted field still reports its **declared** type
(`password: String = <redacted>`), because the type name comes from the declaration, not from the value.

---

## Configuration

`Configuration` is a flat options record, always constructed with named arguments:

```scala
Configuration(multiline = true, useTypeNames = true)
```

| Field | Default | Meaning |
| --- | --- | --- |
| `useFieldNames` | `true` | Render `name = value` rather than a bare `value`. Also gates the type name. |
| `useTypeNames` | `false` | Render the field's declared type between name and value. |
| `fullyQualifiedClassName` | `false` | Use fully qualified names for the type and for field types. |
| `shortPackagePrefix` | `true` | With the above, compress leading lowercase segments: `c.w.d.User`. |
| `fieldsSeparator` | `", "` | Separator **between fields**. Never used between collection elements. |
| `fieldNamePrefix` | `""` | Wraps the field name. |
| `fieldNameSuffix` | `""` | Wraps the field name. |
| `fieldNameAndValueSeparator` | `" = "` | Between the name (and type) and the value. |
| `fieldNameAndTypeNameSeparator` | `": "` | Between the name and the type. |
| `typeNamePrefix` | `""` | Wraps the type name. |
| `typeNameSuffix` | `""` | Wraps the type name. |
| `valuePrefix` | `""` | Wraps every rendered value, including `null`. |
| `valueSuffix` | `""` | Wraps every rendered value, including `null`. |
| `multiline` | `false` | Force the multiline layout. |
| `multilineIndent` | `"  "` | Per-field indent in the multiline layout. |
| `multilineIfFieldsAreGreaterOrEqual` | `5` | Switch to multiline at this many rendered fields. |

`Configuration.default` is the canonical instance and the default argument of `asString` and
`ToString.derived`. It is deliberately **not** also published as a `given` in `Configuration`'s
companion: an instance sitting in `Configuration`'s implicit scope would always win implicit search,
which would make those default arguments unreachable dead code, would make `AutoToString` behave
differently here than in `describo`, and would silently satisfy a downstream `using Configuration`
that a caller had simply forgotten to provide. Supply your own `given Configuration` where you want
one — including at the definition site of any class that mixes in `AutoToString`.

This record keeps **default arguments** even though the repository's Scalafix guidance discourages them.
That is a single, narrow, reviewed exception: a sixteen-field options record without defaults is
unusable, and the call sites are always named arguments, which is the one shape where a flat record of
booleans stays self-documenting. It is also kept field-for-field identical with
`com.worxbend.describo.Configuration`; any change here must be mirrored there and in both READMEs.

### `multilineIfFieldsAreGreaterOrEqual` and its sentinel

Multiline is chosen once per render, if and only if:

```
multiline == true
  || (multilineIfFieldsAreGreaterOrEqual > 0 && renderedFieldCount >= multilineIfFieldsAreGreaterOrEqual)
```

- `renderedFieldCount` is counted **after** exclusion, so excluded fields never push a type over the
  threshold.
- Any value **`<= 0` disables the threshold** entirely — `0` and `-1` both mean "never switch on count".
- A type with **zero** rendered fields is always single line, even under `multiline = true`: it renders
  `T()` with no newlines.

### `fieldsSeparator`

- Single line: fields are joined with `fieldsSeparator` **verbatim**. `" | "` gives `a = 1 | b = 2`.
- Multiline: each field is prefixed with `multilineIndent` and the fields are joined with
  `fieldsSeparator.stripTrailing() + "\n"`. Only **trailing** whitespace is dropped, because it would
  otherwise be invisible trailing whitespace at end of line. Leading whitespace survives, so `" | "`
  makes lines end with ` |`.
- With zero rendered fields the separator is not used at all.

---

## Output reference

| Input | Output |
| --- | --- |
| `String` | `"quoted"`, escaped |
| `Char` | `'c'`, escaped |
| numbers, `Boolean`, `BigDecimal`, `BigInt` | bare, via `toString` |
| `java.time.*` and other opaque types | bare, via `toString` |
| `List`, `Vector`, `Set`, `Seq`, `IndexedSeq`, `Iterable`, `Array` | `["a", "b"]` |
| `java.util.List` / `Set` / `ArrayList` / `HashSet` / `LinkedList` | `["a", "b"]` |
| `Map`, `java.util.Map`, `java.util.HashMap` | `["key" -> "value"]` |
| `Option` | `Some("payload")` / `None` |
| case class | `Type(field = value)` |
| empty or fully excluded case class | `Type()` |
| case object, singleton enum case | `Blue` — bare name, no parentheses |
| sealed family / enum branch, generic or not | the branch's own rendering: `Right(value = 1)` |
| value class | the payload's rendering; its declared type name stays the value class's own |
| `null`, anywhere | `null` |

Collection elements are always joined by a literal `", "`. `fieldsSeparator` is an inter-**field** knob
and is never used inside a collection.

### Escaping

Applied in this order, at every position — top-level field, collection element, `Map` key, `Map` value,
`Option` payload:

`\` → `\\`, `"` → `\"`, newline → `\n`, carriage return → `\r`, tab → `\t`.

Chars use the same table inside single quotes, plus `'` → `\'`. There is deliberately no unicode
escaping and no general control-character handling: the rule is small enough that the two modules
implement it identically.

### `null`

A `null` field renders as the four characters `null`, unquoted, in every position — top-level field,
`Option` payload, collection element, `Map` key, `Map` value, value-class payload. Never `"null"`,
never `None`, never an empty string, never an exception. A `null` collection, `Map`, `Option` or
`Array` renders `null`, **not** `[]` or `None`. `valuePrefix` / `valueSuffix` still wrap it: with
`valuePrefix = "["`, a null field renders `[null]`.

Nullness has no effect on the printed type name, because the name comes from the declared type.

### Type names

Under `useTypeNames` the printed type is the **declared** type, dealiased and widened, with type
arguments dropped: `Int`, `String`, `List`, `Map`, `Option`, `BigDecimal`, `LocalDate`. Runtime classes
never leak — you will not see `Integer`, `Some`, `$colon$colon` or `Map2`. The qualified spelling is the
dealiased type symbol's full name with any trailing `$` removed, e.g. `scala.Int`, `java.lang.String`,
`scala.collection.immutable.List`, `scala.math.BigDecimal`.

Package compression splits the qualified name on `.`, compresses every **leading** segment whose first
character is lowercase to that single character, leaves the rest intact and rejoins:
`com.worxbend.dscrbo.User` → `c.w.d.User`; `com.worxbend.dscrbo.Outer.Inner` → `c.w.d.Outer.Inner`; a
name with no packages is returned unchanged, with no leading dot.

---

## What the macro does, and what it cannot do

Everything decidable from the declared types is decided **once, at expansion time**: which fields are
omitted, which are redacted and with what replacement, how each declared type is spelled, and which
renderer each field's type needs. What reaches the bytecode is straight-line code — direct field
accessor calls and literal strings handed to a handful of runtime helpers. There is no reflection, no
`productElementNames`, and no per-call map building.

Only the `Configuration` reads happen at runtime, because a `Configuration` is an ordinary value that
the caller may vary per call. All three spellings of every type name are emitted as compile-time
literals and selected by a trivial branch.

### Resolution order for a field's type

For every field, in this order:

1. a **primitive**, `String` or `Char` — rendered directly;
2. the **root's own instance**, if the field's type is the type being derived (this is what makes a
   self-recursive type work without unrolling);
3. a **summoned `Describe`** — a user-written instance always wins, including for `Option`, `Map`,
   `Array` and collection types;
4. the **structural handlers** — `Option`, Scala and Java collections and maps, `Array`;
5. the **shape handlers** — case object, sealed family or enum, value class, case class;
6. otherwise the type's own `toString`, but only if that is safe (see below).

Redaction composes through every one of those layers, including `Map` keys, tuple slots, `Array`
elements and sealed-family branches.

### Sealed families and enums, including generic ones

`Either[A, B]`, `scala.util.Try[A]`, and any user `sealed trait F[A]` or `enum E[A]` are rendered by
dispatching on the runtime branch and instantiating each child at the parent's type arguments. The
child types come from the `Mirror.SumOf` the compiler synthesises for the parent; the mirror is read at
expansion time and discarded, so nothing about it reaches the generated code and the zero-dependency
guarantee is untouched. If a family's children cannot be instantiated, the macro says so and names the
child, instead of failing inside the compiler's inliner.

### Fail closed: types the macro cannot see into

A **concrete, final-in-practice class** the macro cannot see into — `java.time.LocalDate`, `BigDecimal`,
`Throwable`, an opaque type — is rendered with its own `toString`. That is safe: nothing else can be
substituted for it at runtime.

A field whose declared type is **abstract** — a non-sealed trait, an abstract class, an abstract type
member, `Any`, `AnyRef` — is a **compile error**, not a `toString`. The runtime value of such a field
may be any subtype, including a case class with `@Redacted` fields, and rendering it with `toString`
would print those in the clear. The error names the type and offers the three ways out: provide a
`given Describe[T]`, seal the hierarchy, or mark the field `@Excluded`. `describo` refuses the same
shapes (magnolia has no `Mirror` for a non-sealed trait), so the two modules fail the same way.

### Recursion and the nesting cap

A self- or mutually recursive type is fully supported when it is **the type being derived**, because
the generated instance calls itself. A recursive type that appears only *inside* another type —
`Root → B → C → B` — cannot be inlined; the macro reports a compile error naming the type. Add
`derives Describe` to it, or provide a `given Describe[B]`, and it works.

Non-recursive nesting is bounded by **two** caps. Both name **the type that is actually too deep** and
ask for a `given Describe` for *that* type — advice that works, because implicit search is consulted
before structural inlining.

| Cap | Value | What it counts |
| --- | --- | --- |
| types | 12 | case classes, value classes and sealed families entered below the root |
| layers | 20 | layers of generated code, which is the above **plus** one per `Option`, collection, `Map` or `Array` wrapper |

The type cap is the one to reason about: twelve means twelve types deep, and a wrapper is not a type.
The layer cap exists for one reason — past roughly two dozen layers the Scala 3 staging phase overflows
a default 1 MB compiler stack — and it is calibrated below every shape measured to do so, so a runaway
model is reported by this macro rather than by a `StackOverflowError` in someone else's build. In
practice: twelve levels of plain nesting, ten through `Option` or a collection, five through
`Option[Map[String, List[_]]]`.

Both are deliberately conservative, and neither is a limit you should be near. Real models give their
inner types their own instance, which costs one layer instead of unrolling the whole subtree.

One limit no depth cap can express: because the macro emits fully unrolled straight-line code, a model
that is both very wide and very deep can exceed the JVM's 64 KB class-size limit and fail to emit. The
remedy is the same one — `derives Describe` on an inner type.

### Remaining limits

- **Generic case classes with `derives`**: `derives Describe` on `Box[A]` derives against the abstract
  `A`, which is now a compile error rather than a silent `toString`. Derive at the instantiated type
  instead — `Describe.derived[Box[String]]`.
- **Indentation of nested multiline renders**: a nested render is inserted **verbatim** and is not
  re-indented relative to its parent. Deeply nested values read best with a single-line configuration.

A type the macro cannot derive is rejected cleanly at compile time with a single error naming the type,
rather than a cascade of secondary errors.

---

## Compatibility

Version `0.1.0-SNAPSHOT` has never been published, and this revision breaks source compatibility on
purpose:

- the package moved from `io.worxbend.describo` to `com.worxbend.dscrbo`, so the two sibling artifacts
  no longer collide on one classpath;
- `Configuration` and `annotations` moved out of `object ToString` and are now top level in
  `com.worxbend.dscrbo`;
- `Configuration.fieldsSeparator` is now genuinely read, and gained `fullyQualifiedClassName` and
  `shortPackagePrefix` for parity with `describo`;
- `object Main` was removed;
- nested strings are now quoted, `Char`s are now quoted, `Option` payloads are rendered rather than
  `toString`-ed, and `useTypeNames` reports declared types instead of runtime classes. Output changes
  in all of those cases, and every change is a bug fix;
- `Configuration`'s companion no longer publishes a `given Configuration`. Code that relied on it being
  found implicitly — including `AutoToString` subclasses — must now bring its own `given Configuration`
  into scope, exactly as `describo` already required;
- a field whose declared type is a non-sealed trait, an abstract class or `Any` no longer renders via
  `toString`; it is a compile error. That closes a plaintext-secret leak, and the fix is to supply a
  `given Describe` for the type, seal the hierarchy, or exclude the field.
