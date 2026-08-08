# Decision record: keep both `toString` renderers, or converge?

**Status:** 🟢 Answered — both are kept, with `dscrbo` as the primary library.
**Scope:** [`libs/commons/describo`](../../libs/commons/describo),
[`libs/commons/dscrbo`](../../libs/commons/dscrbo).

---

## The question

> **Does any actual or planned consumer refuse a magnolia dependency?**
>
> **Answer:** Effectively yes. We would rather not depend on magnolia. `describo` is kept because it
> is useful for some use cases, but **`dscrbo` — the zero-dependency renderer — is the main library**
> and the one investment goes into.

Everything below follows from that answer.

---

## Decision

**Keep both. `dscrbo` is primary; `describo` is the secondary option.**

- **`dscrbo` is the default choice** for new code and the module that gets the attention. Its
  zero-dependency property is the reason it exists and the reason it wins by default.
- **`describo` stays** for the cases it genuinely serves better — principally code that already has
  magnolia on the classpath, and generic case classes, which `dscrbo` cannot derive
  (see [Known divergences](tostring-rendering-spec.md#known-divergences)).
- **Do not shrink `dscrbo`'s charter.** An earlier draft of this record recommended narrowing it to
  flat-to-shallow case classes on the assumption it was the fallback. That assumption was wrong: as
  the primary library its type coverage is exactly what matters most, and closing its remaining gaps
  against `describo` is worth doing rather than declaring out of scope.
- **Do not delete `describo`.** It is the reference implementation that makes the conformance kit
  meaningful. Two independent engines agreeing byte for byte is what catches specification drift; a
  single engine cannot disagree with itself.

---

## Supporting evidence

At the time of writing there is **no production consumer of either module**. Both artifacts are
`0.1.0-SNAPSHOT` and neither has been published:

```bash
# Match directory boundaries, so that libs/commons/describo-tck is not swallowed by the
# libs/commons/describo prefix. The kit is a test-scope dependency of both test modules,
# not a consumer of either renderer.
grep -rn "describo\|dscrbo" --include="*.scala" --include="*.mill" . \
  | grep -vE '^\./(out|libs/commons/(describo|dscrbo|describo-tck)/)'
```

So the choice above is forward-looking: it is about the library we want to invest in, not about
consumers that must be supported today. The upside is that the cost of changing course is currently
near zero, and only rises after the first release.

---

## What you have

Two implementations of one specification
([`tostring-rendering-spec.md`](tostring-rendering-spec.md)) producing byte-identical output for
everything except two documented divergences, enforced by `libs/commons/describo-tck`.

| | `dscrbo` (primary) | `describo` (secondary) |
| :-- | :-- | :-- |
| Mechanism | Scala 3 inline macro, fully unrolled | magnolia `AutoDerivation` |
| Runtime dependencies | **none** | magnolia |
| Main source | ~890 lines / 5 files | ~570 lines / 9 files |
| Type coverage | open — any concrete class renders via its own `toString` | closed — needs an instance per type |
| Generic case classes | ❌ | ✅ |
| Extension point | a `given Describe[T]` | a `given Printable[T]`, or one of four factories |
| Failure at the edges | compile-time refusal at 12 nested types / 20 emitted layers; separately, very wide-and-deep models can exceed the JVM's per-method bytecode limit | `StackOverflowError` at render time, unbounded |
| Compile cost | unrolled emission per derived type | ordinary implicit search |

### On the size limit

`dscrbo`'s hard ceiling is the JVM's **65,535-byte `Code` attribute limit, which applies per method**,
not a class-size limit. A single derived rendering is emitted as one unrolled expression inside one
method, so a model that is both very wide and very deep can exceed it; Scala 3.8.4 reports this as a
"Method too large" error at emission.

That boundary is imposed by the JVM and is distinct from the library's own two caps
(`MaxNestedTypes = 12`, `MaxEmittedLayers = 20`), which are fixed constants chosen to keep the
compiler's staging phase off a `StackOverflowError`. Neither cap can express the bytecode limit,
because bytecode size depends on field count and width as well as depth.

---

## Consequences of choosing `dscrbo` as primary

1. **Its remaining capability gaps are now defects, not acceptable limitations.** Generic case-class
   derivation is the notable one: `describo` handles `Box[A] derives Printable`, `dscrbo` does not,
   because its synthesised `derived$Describe[A]` needs a `Describe[A]` and the module ships no
   per-type instances. Closing that is worth real effort.
2. **The per-method bytecode ceiling deserves engineering attention**, not just documentation. Full
   unrolling is what causes it, and the Scala 3 documentation independently advises avoiding large
   generated methods for JIT reasons. Emitting shared or recursive helpers instead of one flat
   expression would address both at once.
3. **The conformance kit stays**, and `describo` stays with it. Parity is what keeps the primary
   library honest.

---

## What not to do

- ❌ **Do not extract a shared `describo-core`** holding `Configuration`, `FieldRule`, `Layout` and
  `Rendering`. That hands `dscrbo` a runtime dependency and destroys the one property that made it
  the primary choice. The duplication that actually costs is the *semantics*, and the conformance kit
  already owns those.
- ❌ **Do not merge the artifacts.** Two output engines behind one API is strictly worse than two
  APIs, because it moves the choice from compile time to runtime.

---

## Review by

Revisit if `describo` acquires a consumer that `dscrbo` could serve, or if the per-method bytecode
ceiling starts biting real models rather than synthetic ones.
