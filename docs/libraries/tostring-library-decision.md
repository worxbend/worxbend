# Decision record: keep both `toString` renderers, or converge?

**Status:** 🟡 Open — blocked on one question that only a human can answer.
**Scope:** [`libs/commons/describo`](../../libs/commons/describo),
[`libs/commons/dscrbo`](../../libs/commons/dscrbo).

---

## The question

> **Does any actual or planned consumer refuse a magnolia dependency?**
>
> **Answer:** _(unanswered — fill this in)_
> **Answered by:** _(name)_ **on** _(date)_

Everything else about these two modules follows from that one answer, which is why it is at the top of
this page rather than buried in a recommendation.

---

## Evidence gathered

**There is currently no consumer of either module.** A repository-wide search for imports or module
dependencies finds only the modules themselves and the shared conformance kit:

```bash
grep -rn "describo\|dscrbo" --include="*.scala" --include="*.mill" . \
  | grep -v "^./libs/commons/describo\|^./libs/commons/dscrbo\|^./out/"
# (no results)
```

Both artifacts are `0.1.0-SNAPSHOT` and neither has ever been published. So today the question is
entirely forward-looking: it is about consumers you intend to have, not consumers you must support.

That also means the cost of converging is currently **zero**, and it only ever goes up. After the first
release, deleting a module becomes a breaking change for someone.

---

## What you actually have

Two implementations of one specification
([`tostring-rendering-spec.md`](tostring-rendering-spec.md)), producing byte-identical output enforced
by `libs/commons/describo-tck`.

| | `describo` | `dscrbo` |
| :-- | :-- | :-- |
| Mechanism | magnolia `AutoDerivation` | Scala 3 inline macro, fully unrolled |
| Runtime dependencies | magnolia | **none** |
| Main source | ~570 lines / 9 files | ~890 lines / 5 files |
| Type coverage | closed — an instance per type | open — any concrete class renders |
| Generic case classes | ✅ | ❌ |
| Extension point | a `given`, or one of four factories | a `given` |
| Failure at the edges | `StackOverflowError` at render time | compile error at 12 types / 20 layers; 64 KB class-size ceiling |
| Compile cost | ordinary implicit search | unrolled emission per derived type |

---

## Recommendation

**`describo` is the core; `dscrbo` survives only if the answer above is "yes".**

Reasoning:

1. `dscrbo`'s *only* advantage is the zero-dependency property. Every other axis favours `describo`: it
   composes as an ordinary typeclass, it handles generic case classes, its extension point is a
   one-liner, its compile cost is predictable, and its failure modes are ordinary.
2. `dscrbo`'s macro is the higher-maintenance asset in this repository by a wide margin. It needs two
   empirically calibrated stack caps, a `Mirror.SumOf` round-trip to instantiate generic children, a
   fail-closed rule for abstract types, and a cycle stack — and it still carries a 64 KB class-size
   ceiling that no cap can express. That is a lot of machinery to avoid one small dependency.
3. Symmetry is a liability once the two engines drift, and they already did once: they silently
   disagreed on repeated `@Redacted` while both test suites were green. The conformance kit exists
   because of that, and it is now the thing making "keep both" affordable.

### If the answer is **no**

Delete `dscrbo`. Keep `describo-tck` as `describo`'s regression suite — the catalogue is valuable
independently of parity, and 66 obligations expressed as data are worth more than the prose they
replaced.

### If the answer is **yes**

Keep `dscrbo`, but **shrink its charter**. State in its README that it targets flat-to-shallow case
classes with no external dependency, and stop chasing `describo`'s full type coverage. A meaningful
fraction of the macro's complexity exists to reach parity on shapes a dependency-averse consumer is
unlikely to render.

---

## What not to do

- ❌ **Do not extract a shared `describo-core`** holding `Configuration`, `FieldRule`, `Layout` and
  `Rendering`. That hands `dscrbo` a runtime dependency and destroys the one property it has. The
  duplication that costs you is the *semantics*, and the conformance kit already owns those.
- ❌ **Do not merge the artifacts.** Two output engines behind one API is strictly worse than two APIs,
  because it moves the choice from compile time to runtime.

---

## Review by

Revisit within one quarter of the first release of either artifact, or immediately if a consumer
appears. Until the question at the top is answered, "keep both" is the status quo — and it is
defensible precisely because the kit makes drift a build failure rather than a surprise.
