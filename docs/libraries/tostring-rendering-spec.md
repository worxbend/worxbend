# `toString` rendering specification

**Normative reference for [`describo`](../../libs/commons/describo) and
[`dscrbo`](../../libs/commons/dscrbo).**

These two libraries are separate implementations of the single specification written down here. For
the same input and an equivalent `Configuration` they must produce **byte-identical** output — with
exactly two carved-out exceptions, listed under [Known divergences](#known-divergences) and pinned by
`KnownDivergenceSuite` in each module:

1. an enum case's **qualified** name (simple names agree);
2. generic case classes, which `describo` derives and `dscrbo` cannot.

Outside those two, any difference is a bug in whichever engine deviates. The parity claim is
deliberately narrow rather than aspirational: a contract with unlisted exceptions is not a contract.

This page is the source of truth for the rules below. Each module's README covers how to *use* that
module — installation, derivation styles, macro or magnolia specifics — and defers to this page for
what the output actually is. When a rule here and a README disagree, this page wins and the README is
a bug.

> **The rules on this page are enforced, not just written down.** `libs/commons/describo-tck` encodes
> them as a catalogue of `(fixture, configuration, expected string)` obligations that both renderers
> execute. Changing a rule here without changing the kit means the kit and the prose disagree; changing
> the kit without changing both engines breaks the build.

---

## Contents

- [Configuration](#configuration)
- [Annotations](#annotations)
- [Value rendering](#value-rendering)
- [Escaping](#escaping)
- [`null`](#null)
- [Type names](#type-names)
- [Layout](#layout)
- [Known divergences](#known-divergences)

---

## Configuration

Both modules declare a `Configuration` with these sixteen fields, in this order, with these defaults.
The records are duplicated deliberately — `dscrbo` cannot depend on `describo` without losing its
zero-dependency guarantee — so **any change here must be made in both**, and in the kit's
`TckConfiguration`.

A field renders as:

```text
fieldNamePrefix name fieldNameSuffix
  [ fieldNameAndTypeNameSeparator typeNamePrefix Type typeNameSuffix ]
  fieldNameAndValueSeparator valuePrefix value valueSuffix
```

The bracketed group appears only under `useTypeNames`. The whole name group is dropped when
`useFieldNames` is off — including the type name, which belongs to it.

| Option | Default | Meaning |
| :-- | :-- | :-- |
| `useFieldNames` | `true` | render `name = value` rather than a bare `value` |
| `useTypeNames` | `false` | render each field's **declared** type |
| `fullyQualifiedClassName` | `false` | qualified names instead of simple names, for the product's own name and field type names alike |
| `shortPackagePrefix` | `true` | compress leading lowercase segments to one character: `com.worxbend.example.Order` → `c.w.e.Order`. Ignored unless `fullyQualifiedClassName` is set |
| `fieldsSeparator` | `", "` | between fields; used **verbatim** on a single line |
| `fieldNamePrefix` | `""` | before each field name |
| `fieldNameSuffix` | `""` | after each field name, ahead of any type name |
| `fieldNameAndValueSeparator` | `" = "` | between the name group and the value |
| `fieldNameAndTypeNameSeparator` | `": "` | between field name and type name; only under `useTypeNames` |
| `typeNamePrefix` | `""` | before each type name |
| `typeNameSuffix` | `""` | after each type name |
| `valuePrefix` | `""` | before every value, `null` and redaction replacements included |
| `valueSuffix` | `""` | after every value, `null` and redaction replacements included |
| `multiline` | `false` | force one field per line |
| `multilineIndent` | `"  "` | per-field indent in the multiline layout |
| `multilineIfFieldsAreGreaterOrEqual` | `5` | switch to multiline at this many **rendered** fields |

---

## Annotations

| Annotation | Effect | Value dereferenced? |
| :-- | :-- | :-- |
| `@Redacted` | renders `<redacted>` | never |
| `@Redacted("x")` | renders `x` | never |
| `@Excluded` | field omitted entirely | never |
| `@transient` | exact alias of `@Excluded` | never |

### Precedence

Resolved once per field — at `join` time in `describo`, at expansion time in `dscrbo` — first match
wins:

1. `@Excluded` **or** `@transient` → omit the field
2. `@Redacted` → render the replacement
3. otherwise → render normally

**Exclusion beats redaction.** `@Redacted @Excluded both: String` is omitted, not redacted, and the
source order of the two annotations is irrelevant.

When a field carries several `@Redacted`, **the one written first in source order wins**. Magnolia
surfaces `param.annotations` in reverse source order, so `describo` reverses before searching; without
that the two engines disagree, which is exactly the bug that motivated the conformance kit.

### Values are never read

An omitted or redacted field's value is never dereferenced — not for the value, not for a type name,
not for a `null` check. This is what makes a `null` secret safe and what stops a redacted field from
being touched at all.

A value class's sole parameter follows the same rules, except that `@Excluded` on it is ignored: there
would be nothing left to render.

---

## Value rendering

| Shape | Output |
| :-- | :-- |
| `String` | `"quoted"` |
| `Char` | `'q'` |
| numeric primitives, `Boolean` | bare — `1`, `2.5`, `true` |
| `BigDecimal` | scale preserved — `1000.50` |
| `BigInt` | bare |
| `List`, `Vector`, `Set`, `Seq`, `IndexedSeq`, `Iterable`, `Array` | `[a, b]` |
| `Map` | `["k" -> "v"]`, both sides by these same rules |
| `Option` | `Some(x)` / `None` |
| `java.util` lists, sets, maps | identical to their Scala counterparts — maps in `[...]`, never `{...}` |
| `java.time` values | their own `toString` |
| nested case class | inline, through these same rules |
| case object, parameterless enum case | its bare name, no parentheses |
| value class | unwrapped to its payload |
| empty product, or one whose fields are all omitted | `Name()` |

---

## Escaping

Inside a `String`: `\` first, then `"`, `\n`, `\t`, `\r`. Inside a `Char`: the same, plus `'`.

Escaping applies at every level — a string inside a list inside a map is escaped the same way as a
top-level field — so a value containing a comma or a parenthesis can never be confused with structure.

---

## `null`

`null` renders as the bare word `null` in **every** position: field, `Option` payload, collection
element, map key, map value, value-class payload. Never `"null"`, never `None`, never an exception.

- a `null` collection is `null`, not `[]`
- a `null` `Option` is `null`, and is distinct from `None`
- `valuePrefix`/`valueSuffix` still wrap it
- a `null` in a `@Redacted` field renders the replacement, because the value is never read

---

## Type names

Under `useTypeNames` the **declared** type is rendered — never the runtime class:

```text
roles: List   = ["admin"]      not  roles: $colon$colon
meta:  Map    = ["k" -> "v"]   not  meta: Map1
opt:   Option = Some("v")      not  opt: Some
count: Int    = 1              not  count: Integer
```

The canonical rule is `TypeRepr.of[X].dealias.typeSymbol.fullName` with a trailing `$` stripped.
`dscrbo` computes it in the macro; `describo` carries it on `Printable.printedType`, which is what lets
its render path avoid dereferencing values entirely.

---

## Layout

The multiline layout applies when `multiline` is set **or** when the number of **rendered** fields
reaches `multilineIfFieldsAreGreaterOrEqual`. Excluded and transient fields do not count toward it.

> `multilineIfFieldsAreGreaterOrEqual <= 0` **disables** the threshold. It does not mean "always".
> Use `multiline = true` for that, and `-1` as the idiomatic "always one line".

A product with no rendered fields stays on one line regardless — there is nothing to break across
lines.

`fieldsSeparator` is used verbatim on a single line. In the multiline layout its trailing whitespace is
stripped before the newline, so `", "` yields `",\n"` rather than a trailing space on every line.

**A nested value is inserted verbatim and is not re-indented.** Under `multiline`, a nested product's
own closing parenthesis therefore sits at the outer indent level:

```text
Outer(
  inner = Inner(
  v = 1
),
  tag = "t"
)
```

This is ugly and both engines do it identically. It is pinned by the kit so it cannot change in only
one of them.

---

## Known divergences

Two, deliberate, pinned by `KnownDivergenceSuite` in both modules so neither can quietly become three.

| | `describo` | `dscrbo` |
| :-- | :-- | :-- |
| **Enum case, qualified name** | `com.example.Red` — magnolia's `TypeInfo` reports the enclosing *package* | `com.example.Colour.Red` — the macro reads the case symbol |
| **Generic case class** | ✅ derives at the instantiated type | ❌ `derived$Describe[A]` needs a `Describe[A]`, and the module ships no per-type instances by design |

Enum **simple** names agree; only `fullyQualifiedClassName` is affected.

Everything else on this page is shared, and any difference is a bug in whichever engine deviates.
