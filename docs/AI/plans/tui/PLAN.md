# Development Plan: Scala 3 TUI library

**Status**: draft, ready to hand to an AI coding agent for autonomous implementation.
**Prerequisite reading**: [`RESEARCH.md`](RESEARCH.md) (analysis of ratatui, TamboUI,
Terminus, cue4s, Textual — read before writing any code, it justifies most decisions
below) and [`SPEC.md`](SPEC.md) (the concrete type/trait signatures this plan's
modules are specified against — where the two disagree, `SPEC.md` is authoritative
since it was written second, incorporating deeper research; this file has one known
superseded detail, flagged inline in §5 below).

## 1. Goal

Build a Scala 3 library for building rich terminal user interfaces (TUI) on the JVM,
with:

- A **modular, multi-module Mill build**, following `worxbend`'s conventions
  (`SbtModule` + `PublishModule`, ScalaTest) adapted to a standalone repo — see §3.
- A **GraalVM native-image compilation target** for at least the demo/example apps.
- A **rich widget ecosystem**, drawing the widget catalog from TamboUI/ratatui.
- A convenient, idiomatic **Scala 3 DSL** for declaring UIs — spiritually close to
  JetBrains Compose (declarative, composable, retained-mode) but expressed as a
  real Scala 3 DSL (extension methods, given/using, contextual builders, opaque types),
  not a straight port of Compose's Kotlin API or TamboUI's Java fluent-builder API.

## 2. Engineering principles (read before writing any code)

### 2.1 We are not porting ratatui/TamboUI/Terminus/cue4s/Textual — we are designing our own, better library

`RESEARCH.md`'s clones exist **only** to understand architecture, API shape, and
lessons learned (what worked, what a project's own comments flag as unfinished —
e.g. Terminus's `React` capability being explicitly a "stub" per its own source
comments, `RESEARCH.md`). They are reference material, never a source to copy code
from:

- **No copy-pasted implementation code** from any cloned repo, in any language.
  Every type in `SPEC.md`, every algorithm (the constraint solver, the buffer diff,
  the `CharWidth` logic, the signal dependency-tracking mechanics) must be written
  from scratch against the *documented behavior/shape*, not translated line-by-line
  from Rust/Java/Python source. Where `RESEARCH.md`/`SPEC.md` quote a signature from a
  reference implementation, that quote is there to justify a design decision in
  prose — it is not a stand-in for an implementation.

  **Clarification — what this rule does *not* cover** (so it doesn't collide with
  the §12 risk mitigations that reference external material):
  - *Standards-derived data tables are data, not code.* `CharWidth`'s width table is
    generated from the Unicode Character Database (East Asian Width, general
    categories) — deriving that table from the UCD, or checking it against another
    library's UCD-derived table, is permitted and encouraged; hand-transcribing
    another library's *code* (its lookup/branching logic) is what's forbidden.
  - *Replicating test coverage is not copying tests.* Where §12 says to consult
    Terminus's signal tests, that means: identify the coverage *categories* their
    suite proves (conditional dependencies, unsubscribe-on-recompute, `peek` vs.
    `get` semantics) and write original tests covering the same categories — not
    port their test code.
- Where an idea is adopted (e.g. Terminus's signals model, TamboUI's render-thread
  guard, Textual's headless-driver testing pattern), the goal is to reimplement it
  **better**: idiomatic Scala 3 (not a Java-shaped or Python-shaped API wearing Scala
  syntax), with the gaps the reference implementations themselves flag as unfinished
  or admit as tech debt closed, not reproduced. Treat every "worth stealing" note in
  `RESEARCH.md` as "worth improving on," not "worth cloning."
- If an implementing agent finds itself with a reference repo's source file open
  side-by-side while writing a `tui-*` module, that is a signal to close it and work
  from `SPEC.md`'s signatures and `RESEARCH.md`'s prose description instead — precise
  behavioral requirements are already captured there.

### 2.2 Use this repo's OOP/design-review discipline throughout

Apply the guidelines and, where available, the Claude Code skills from
[w0rxbend/ai-oop-design-patterns](https://github.com/w0rxbend/ai-oop-design-patterns)
for every non-trivial design decision in this project — it's the house style for
object-oriented Scala design in this ecosystem, purpose-built for exactly this kind of
"design a clean, extensible system, not a pile of copy-pasted integration code" work:

| When | Apply |
|---|---|
| Structuring a new module/type (e.g. `Backend`, `Reactive`, `Element`) | `oop-design` skill — the four pillars as failure detectors |
| Any design or code review pass | `solid-principles` skill — SRP/OCP/LSP/ISP/DIP checklist |
| Deciding how to make something extensible (new widget, new backend, new event source) | `gof-patterns` skill — problem → pattern decision tables (this project is exactly the kind of "how do components get composed/created/communicate" system the catalog targets — e.g. `Backend` implementations are a textbook Strategy/Bridge case, `Reactive`/`Computed` is Observer-shaped, `Element` construction is Builder-shaped) |
| Any code that grows tangled or hard to follow | `refactoring` skill — code-smell catalog + safe incremental refactorings |
| Any Scala code, always | `scala-style` skill — this project already has its own `SCALA_CODE_STYLE.md` at the repo root; treat the two as complementary (this repo's guide is authoritative for anything it covers; fall back to the `ai-oop-design-patterns` guide for direct-style-Scala/DI/concurrency concerns it doesn't) |

**How to get these skills active**: clone
`https://github.com/w0rxbend/ai-oop-design-patterns` and either (a) symlink its
`skills/*` directories into this repo's `.claude/skills/` the way that repo does it
for itself, or (b) if skill-symlinking isn't set up, read the relevant `SKILL.md` +
linked guideline docs directly before the design decision in question — the
`SKILL.md` files are self-contained enough to follow without the symlink machinery.
Either way, this is a standing instruction for the whole `tui-*` build, not a one-time
setup step — re-apply the SOLID/GoF checks at each module boundary in `PLAN.md` §10,
not just once at the start.

## 3. Repository and module layout

**This is a standalone repository, not nested inside `worxbend`'s `libs/`.** It is
built by an AI coding agent in its own repo, following `worxbend`'s conventions
(package naming, `SbtModule`+`PublishModule`, Mill, ScalaTest, `SCALA_CODE_STYLE.md`)
because that's the house style this plan was designed against — but there is no
monorepo root to nest under, so module directories live at the repo root, not under
`libs/tui/`.

- Suggested (optional) repo name, drawn from the unused pool in `applications/NAMES`
  in the `worxbend` monorepo (see its root `README.md`, "A note on naming"):
  **`basilisk`**. This is a naming-convention nod, not a requirement — a published,
  externally-reusable library plausibly benefits more from a discoverable name than
  from the internal-tool obfuscation `worxbend`'s own apps use; the implementing agent
  or its operator may pick either style.
- Repo root has one `build.mill` (root aggregator, see §4.1) and one directory per
  module: `core/`, `terminal/`, `widgets/`, `runtime/`, `dsl/`, `macros/`,
  `test-support/`, `examples/` — each with its own `package.mill`, `src/main/scala`,
  and (where relevant) a `test` submodule. Module *directory* names are short (`core`, not
  `tui-core`) since there's no sibling-module ambiguity to disambiguate against
  outside a monorepo; the **published artifact ids stay `tui-core`, `tui-terminal`,
  etc.** via an explicit `artifactName` override (§4.1) so downstream consumers see
  the intended library name regardless of the repo's internal directory layout.
- Package naming (`io.worxbend.tui.*`) is unchanged from `SPEC.md` §1 — it names the
  published library, not the repo layout, so it doesn't need to shift with the repo
  becoming standalone.

## 4. Module architecture (Mill)

Mirror TamboUI's module boundaries (see `RESEARCH.md`), adapted to Mill's
`package.mill` layout and `worxbend`'s `SbtModule`/`PublishModule` conventions.

| Module dir | Published as | Depends on | Purpose | Spec ref |
|---|---|---|---|---|
| `core/` | `tui-core` | — | `Buffer`, `Cell`, `Rect`, `Style`/`Color`, `Text`/`Span`/`Line`, `Layout`/`Constraint`, `Widget`/`StatefulWidget` traits, `CharWidth` (display-width utility, see RESEARCH — critical, do first). Maximum API stability; everything else depends on this and nothing else may depend on anything else. | `SPEC.md` §2 |
| `terminal/` | `tui-terminal` | `core` | Raw terminal control: raw mode, alternate screen, cursor, ANSI codes, key/mouse event reading. Modeled on Terminus's `core` module vocabulary. This is the backend abstraction layer — one implementation is enough for v1 (JLine 3, matching TamboUI's default and this being the JVM's most mature terminal library — see §4.1 for the pinned coordinate), but keep it behind a `Backend` trait so alternative backends are pluggable later (mirrors ratatui's per-backend crates / TamboUI's `-jline3-backend`/`-aesh-backend`/`-panama-backend` split). | `SPEC.md` §3 |
| `widgets/` | `tui-widgets` | `core` | All built-in widget implementations (see §6 for the backlog). Depends only on `core`, never on `terminal` or the DSL layers — widgets must be terminal-backend-agnostic. | `SPEC.md` §2.6 |
| `runtime/` | `tui-runtime` | `core`, `terminal` | Mid-level framework: the render loop / "runner" (TamboUI's `TuiRunner` equivalent), event dispatch, tick-rate-driven redraws, resize handling, the render-thread model (single dedicated thread; `checkRenderThread()`/`runOnRenderThread()`/`runLater()` guards), and the `Signal`/`Computed` reactive-state primitive. | `SPEC.md` §4 |
| `dsl/` | `tui-dsl` | `core`, `widgets`, `runtime`, `macros` | The high-level declarative Scala 3 DSL (see §5) — the retained-mode component tree, focus management, event routing; consumes `FormSpec`/`ActionHandler` from `macros` for `Form` wiring and action dispatch (`SPEC.md` §6). This is the module application authors are expected to use day-to-day. | `SPEC.md` §5 |
| `macros/` | `tui-macros` | `core` | Scala 3 macros/inline metaprogramming for compile-time codegen (event-handler dispatch, case-class → form derivation) — owns the `FormSpec`/`ActionHandler` result types alongside the inline defs that produce them; see §7, native-image rationale. No runtime reflection anywhere in this module. | `SPEC.md` §6 |
| `test-support/` | not published | `core`, `terminal`, `runtime` | Shared test infrastructure: the `Pilot`-equivalent driver (`pressKey`, `click`, `waitForIdle`, `assertRendered` — §9) and the render-to-`Buffer` ScalaTest matchers (§6). Consumed by other modules' `test` submodules only, never by main sources. Scaffolded in step 1 like every other module (its contents land at step 4, but the empty module exists from the start so the step-1 acceptance criterion's module list is final). | — |
| `examples/` | not published | everything | Runnable example apps, one sub-directory per example (`examples/hello-world/`, …) — also the native-image compile targets, see §8. | — |

Non-goals for v1 (full CSS cascade engine, asyncio-style message bus, screen-stack
navigation, multi-protocol images, `DataTable`/`DirectoryTree`/syntax-highlighted
`TextArea`) are enumerated with rationale in `SPEC.md` §7 — consult it before adding
scope to any module above that isn't already covered by §6's tiered widget backlog.

### 4.1 Mill conventions — concrete, verified example

`worxbend` itself has no existing example of cross-module `moduleDeps` or
native-image wiring to copy from, so this is spelled out explicitly rather than left
for the implementing agent to infer from repo precedent. Verified against Mill's own
docs (mill-build.org) for the Mill 1.x module system (`package.mill`, matching
`worxbend`'s `.mill-version` = `1.1.6`):

**Root aggregator** (`build.mill`), matching `worxbend`'s `libs/package.mill` pattern:

```scala
package build

import mill.*

object `package` extends Module {}
```

**A leaf module with no internal deps** (`core/package.mill`):

```scala
package build.core

import mill.*
import mill.javalib.*
import mill.javalib.publish.*
import mill.scalalib.SbtModule

object `package` extends SbtModule with PublishModule {

  def scalaVersion = "3.7.1"
  def artifactName = "tui-core"

  def pomSettings = PomSettings(
    description = "Foundational types for the tui Scala 3 TUI library: Buffer, Rect, Style, Layout, Widget traits.",
    organization = "io.worxbend",
    url = "https://github.com/worxbend/basilisk", // update to the actual repo URL at kickoff
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("worxbend", "basilisk"), // ditto
    developers = Seq(Developer("limpid-kzonix", "limpid-kzonix", "https://github.com/limpid-kzonix")),
  )

  def publishVersion = "0.1.0-SNAPSHOT"

  object test extends SbtTests with TestModule.ScalaTest {
    def mvnDeps = super.mvnDeps() ++ Seq(mvn"org.scalatest::scalatest:3.2.19")
  }
}
```

**A module depending on another module, with an external dependency**
(`terminal/package.mill`) — this is the piece that resolves the "unverified Mill
mechanics" gap: `moduleDeps` references sibling modules via `build.<name>` (fully
qualified) or the short name if unambiguous, and **`moduleDeps` is a plain `def`, not
a `Task`** — Mill's module graph shape must be knowable without evaluating tasks, so
this cannot be wrapped in `Task.Anon`/`T{}`:

```scala
package build.terminal

import mill.*
import mill.javalib.*
import mill.javalib.publish.*
import mill.scalalib.SbtModule

object `package` extends SbtModule with PublishModule {

  def scalaVersion = "3.7.1"
  def artifactName = "tui-terminal"

  def moduleDeps = Seq(build.core)

  def mvnDeps = super.mvnDeps() ++ Seq(
    // pinned 2026-07; bump to the latest 3.30.x patch at implementation kickoff —
    // do not jump to the JLine 4.x major line for v1, see SPEC.md §3
    mvn"org.jline:jline:3.30.13",
  )

  def pomSettings = PomSettings(
    description = "JLine 3 terminal backend for the tui library.",
    organization = "io.worxbend",
    url = "https://github.com/worxbend/basilisk",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("worxbend", "basilisk"),
    developers = Seq(Developer("limpid-kzonix", "limpid-kzonix", "https://github.com/limpid-kzonix")),
  )

  def publishVersion = "0.1.0-SNAPSHOT"

  object test extends SbtTests with TestModule.ScalaTest {
    def mvnDeps = super.mvnDeps() ++ Seq(mvn"org.scalatest::scalatest:3.2.19")
  }
}
```

Every other module in the table above follows the same shape: `moduleDeps = Seq(...)`
listing its dependency column from the table, `artifactName` set to its "Published
as" column value, `pomSettings`/`publishVersion` copied verbatim from `terminal/`
above (only `description` changes per module). `examples/` sub-modules are
`SbtModule` (no `PublishModule` — they're not published) with `moduleDeps` on
whichever of `core`/`terminal`/`widgets`/`runtime`/`dsl` that specific example needs;
see §7 for the native-image-specific trait they additionally mix in.

**Decision — single `publishVersion` across all modules** (per `SPEC.md` §8): every
module's `publishVersion` stays `"0.1.0-SNAPSHOT"` until the plan's v1 scope
(`PLAN.md` §10, steps 1–11) is complete, then bump together to `"0.1.0"` for the
first real release. Do not let individual modules drift to independent version
numbers pre-1.0.

## 5. DSL design goals

Target ergonomics — a `HelloWorld` example should read close to:

```scala
import io.worxbend.tui.dsl.*

object HelloWorld extends TuiApp:
  def view(using ReactiveScope): Element =
    panel("Hello")(
      text("Welcome!").bold.color(Color.Cyan),
      spacer,
      text("Press 'q' to quit").dim,
    ).rounded
```

> **Superseded detail**: the original sketch of this example (still shown in earlier
> revisions of this plan) had `view(state: State): Element`, implying explicit
> state-passing. That's now superseded by `SPEC.md` §4.1/§5.3: state lives in
> `Signal`/`Computed` values (Terminus's reactive-signals model, `RESEARCH.md`), read
> reactively via a `ReactiveScope` capability parameter rather than passed as an
> explicit argument. The signature above matches `SPEC.md` §5.3 — treat `SPEC.md` as
> authoritative on this point.

Design principles, synthesized from RESEARCH.md:

1. **Retained-mode component tree**, not raw immediate-mode — build on top of
   `tui-runtime`'s render loop, exposing a `view(using ReactiveScope) => Element`
   shape (`SPEC.md` §5.3) backed by Terminus's fine-grained `Signal`/`Computed`
   reactive-signals model (`SPEC.md` §4.1, `RESEARCH.md` Terminus section) — state
   changes mark dependents stale and trigger a re-render on next redraw, with
   automatic dependency tracking (no manual dependency-array bookkeeping, unlike
   React's `useEffect`). Closer to SolidJS/Preact-Signals in spirit than to
   Compose's recomposition model or TamboUI's builder-call-per-frame Toolkit, even
   though the resulting call-site ergonomics (§ below) look Compose-like.
2. **Real Scala 3 DSL, not a builder-pattern port.** Use:
   - extension methods for fluent styling (`.bold`, `.color(...)`, `.rounded`) instead
     of Java-style chained builder calls returning `this`.
   - `given`/`using` for ambient concerns (current `Theme`, current `FocusScope`)
     instead of threading them explicitly through every call.
   - union types / opaque types for constraint shorthand (e.g. a `Constraint` that can
     be built from a plain `Int` (cells), `Double` (fraction), or `Constraint.fill`)
     rather than TamboUI's separate static factory methods per unit.
   - contextual/structural builders (`apply` on companion objects taking `Element*`
     varargs) for containers, so `panel("Title")(child1, child2, ...)` reads naturally.
3. **Effect-agnostic core, optional integration modules** — following cue4s: the core
   event/render API should not hard-depend on a specific effect system. Ship a plain
   synchronous API in `tui-runtime`/`tui-dsl`; consider an optional `tui-cats-effect`
   module later if there's real demand, but do not build it into v1 scope.
4. **Compile-time over reflection** wherever the DSL needs to bridge user code
   (event-handler binding, case-class-derived forms) — see §7.
5. **Unicode-correct by construction** — DSL text APIs must route through `tui-core`'s
   `CharWidth` utility for all width/truncation math; this is not optional polish, get
   it right in `tui-core` before any widget is built on top of it.

## 6. Widget backlog (v1 scope, drawn from TamboUI's catalog)

Implement roughly in this order — each tier depends on the primitives below it and
gives an early, demoable milestone:

**Tier 0 — layout & chrome primitives** (needed by everything else):
`Block`/border, `Row`/`Column` layout containers, `Text`/`Span`/`Line`, `Spacer`,
`Scrollbar`.

**Tier 1 — static content widgets**:
`Paragraph`, `List`, `Table`, `Tabs`, `Gauge`, `LineGauge`, `Sparkline`.

**Tier 2 — interactive input widgets**:
`TextInput`, `Checkbox`, `Toggle`, `Select`, `Tree`, `Form` (composes the above).

**Tier 3 — data viz / drawing**:
`BarChart`, `Chart` (line/scatter), `Canvas` + shape primitives, `Calendar`.

**Tier 4 — nice-to-have / stretch**:
`Spinner`, `WaveText`, `Dialog`, `DualSparkline`, braille drawing, Markdown view,
multi-protocol image widget (Kitty/iTerm/Sixel/half-block — lowest priority, highest
platform-compatibility risk).

**Tier 5 — stretch, beyond the ratatui/TamboUI baseline** (scope only after Tiers 0–4
are solid; sized reference: Textual, RESEARCH.md, is the only one of the four reference
libraries that ships these): a proper `DataTable` (sortable/filterable, distinct from
the simpler Tier 1 `Table`), `DirectoryTree` (filesystem-aware `Tree`), and a
syntax-highlighted `TextArea`/code editor. These are meaningfully harder than the rest
of the backlog (`TextArea` in particular implies a text-editing/undo model and likely a
tree-sitter-equivalent or regex-based highlighter) — do not pull them forward into v1
scope without an explicit decision to do so.

Each widget: implement against the `Widget`/`StatefulWidget` trait in `tui-core`,
ship a scalatest suite asserting on rendered `Buffer` contents (TamboUI has a
`tamboui-core-assertj` module for buffer assertions — build an equivalent lightweight
ScalaTest matcher in the `test-support/` module (§4) rather than pulling in AssertJ),
and get a DSL-facing wrapper in `tui-dsl`.

## 7. GraalVM native-image target

**Decision (was "open" — now resolved): use Mill's built-in `NativeImageModule`
trait.** Verified against Mill's own documentation
(mill-build.org/blog/7-graal-native-executables.html) — this is built into Mill core
as of the version this plan targets, no third-party plugin (e.g. the older
`alexarchambault/mill-native-image`, which predates Mill's current module system and
should **not** be used here) needed:

```scala
// examples/hello-world/package.mill
package build.examples.`hello-world`

import mill.*
import mill.scalalib.SbtModule
import mill.javalib.NativeImageModule

object `package` extends SbtModule with NativeImageModule {

  def moduleDeps = Seq(build.core, build.terminal, build.runtime, build.dsl)

  // Pin to a specific GraalVM distribution/version at implementation kickoff —
  // Mill's JVM-management system downloads it automatically. Match the major
  // Java version to whatever the repo's .sdkmanrc-equivalent settles on.
  def jvmVersion = "graalvm-community:23.0.1"

  def nativeImageOptions = Seq("--no-fallback")
}
```

Invoked as `./mill show examples.hello-world.nativeImage`. Apply the same trait to
every other `examples/*` sub-module once §10 step 10 is reached — no per-example
variation expected beyond `moduleDeps` and possibly extra `nativeImageOptions` if a
specific example's dependencies need resource-bundle inclusion flags (TamboUI's own
`nativeImageOptions`-equivalent config needed exactly this for a translation
resource, per the Mill blog example this snippet is modeled on — treat that as a
preview of the kind of flag that shows up in practice, not a fixed list).

- **Reflection discipline is the load-bearing constraint here** (see RESEARCH.md,
  TamboUI section): anywhere the DSL needs to bridge user code — event-handler
  dispatch, case-class-derived form fields — use Scala 3 `inline`/macros in
  `tui-macros` to generate the glue at compile time, not runtime reflection +
  native-image reflect-config JSON. This determines whether native-image builds stay
  simple or become a maintenance burden; get it right from the first widget that needs
  user-code binding (`Form`, `TextInput` validators), not retrofitted later.
- **Resolved — GraalVM native-image vs. Scala Native**: both Terminus and cue4s
  target Scala Native successfully for a native binary, without needing GraalVM at all.
  GraalVM native-image was specified explicitly by the requester and is now backed by
  a concrete, built-into-Mill mechanism (above), so it is the v1 target, full stop —
  Scala Native remains a documented alternative/stretch goal, not a fallback to weigh
  mid-implementation. Cross-compiling `core`/`terminal` to also build under Scala
  Native later is a smaller lift if the core module stays JVM-reflection-free and
  dependency-light from the start (another reason this section's reflection discipline
  matters even beyond native-image specifically).

## 8. Examples

`tui-examples` should ship, in rough build order (doubling as milestones):

1. `hello-world` — static `Paragraph` in a bordered `Block`. Validates: core render
   loop, terminal backend, DSL `panel`/`text`.
2. `counter` — a stateful widget with keybindings (`+`/`-`/`q`). Validates: event
   loop, state update → re-render cycle, render-thread model.
3. `todo-list` — `List` + `TextInput` + focus switching. Validates: multi-widget
   focus management, `Select`/`List` state.
4. `dashboard` — `Gauge` + `Sparkline` + `Chart` + tick-rate animation. Validates:
   layout composition (`Row`/`Column`/`Constraint`), animation/tick events.
5. `form-demo` — `Form` composing `TextInput`/`Checkbox`/`Select` with validation and
   compile-time-derived binding (exercises `tui-macros`). Validates: the
   reflection-avoidance approach end-to-end before native-image is attempted.

Each example must build with `./mill examples.<name>.run` and (once §7 lands)
`./mill examples.<name>.nativeImage`.

## 9. Testing strategy

- `tui-core`: unit tests per primitive (`Buffer`, `Layout`/`Constraint` solving,
  `CharWidth` — including CJK/emoji/zero-width cases, since this is the module most
  likely to have subtle bugs that only show up with real Unicode input).
- `tui-widgets`: render-to-`Buffer` + assert-on-`Buffer`-contents tests per widget,
  covering both stateless and stateful render paths.
- `tui-runtime`: event-dispatch and render-thread-guard tests (can run without a real
  terminal — TamboUI's thread checks are no-ops when no render thread is registered,
  worth replicating that pattern here for testability).
- `tui-dsl`: DSL-construction tests (build an `Element` tree, assert its shape/props)
  plus a handful of end-to-end tests reusing the `tui-examples` apps' `view` functions.
- **Headless end-to-end testing** (adopted from Textual's `headless_driver.py` +
  `pilot.py`, RESEARCH.md): add a headless `Backend` implementation in `tui-terminal`
  that renders to an in-memory `Buffer` and accepts synthetic key/mouse events instead
  of a real TTY, plus a small `Pilot`-equivalent test helper (`pressKey`, `click`,
  `waitForIdle`, `assertRendered`) in the `test-support/` module (§4). This lets
  `tui-examples` apps be driven end-to-end (press key → assert rendered buffer) in CI
  without a pseudo-terminal. Build this once `tui-runtime`'s event loop exists (step 4
  of §10) — retrofitting it later means every widget test written before it exists gets
  redone.
- All modules: `SbtTests with TestModule.ScalaTest`, per existing repo convention.

## 10. Suggested execution order for the implementing agent

1. Scaffold the Mill module tree (§4) — empty modules, `package.mill` files, root
   aggregator, verify `./mill resolve _` lists every module at the repo root.
2. `tui-core`: `Rect`, `Buffer`/`Cell`, `Style`/`Color`, `CharWidth`, `Constraint`/
   `Layout`, `Widget`/`StatefulWidget` traits. Full unit test coverage before moving on.
3. `tui-terminal`: JLine 3-backed `Backend`, raw mode / alternate screen / cursor,
   key + mouse event reading.
4. `tui-runtime`: render loop, event dispatch, render-thread model. Get `hello-world`
   (immediate-mode, no DSL yet) rendering to a real terminal here as a smoke test.
   Build the headless `Backend` + `Pilot`-equivalent test helper (§9) right after the
   real backend works, so every subsequent step can write end-to-end tests instead of
   only unit tests.
5. `tui-widgets` Tier 0 + Tier 1 (§6).
6. `tui-dsl` v1: retained-mode `Element` tree + the styling/layout extension methods
   from §5, wired to Tier 0/1 widgets. Get the `hello-world` example (§8) running
   through the DSL.
7. `tui-widgets` Tier 2 + `tui-macros` (compile-time event/form binding) together —
   Tier 2 is exactly where reflection temptation shows up first, so build the
   macro-based alternative alongside it, not after.
8. `counter`, `todo-list` examples (§8) as they become buildable.
9. `tui-widgets` Tier 3, `dashboard` example.
10. GraalVM native-image wiring (§7) against `hello-world` first, then the rest of
    `tui-examples` — validate the reflection-free approach actually holds before
    declaring native-image support "done."
11. Tier 4 widgets and `form-demo` as stretch/final polish.

## 11. Acceptance criteria per execution-order step

For each step in §10, "done" means the following, not just "code exists":

| Step | Acceptance criteria |
|---|---|
| 1. Mill scaffold | `./mill resolve _` lists every module in §4 at the repo root; `./mill __.compile` succeeds on empty modules. |
| 2. `tui-core` | `SPEC.md` §2 types implemented; `CharWidth` has explicit test cases for at least: ASCII, CJK (double-width), zero-width combining marks, emoji (including ZWJ sequences), and the empty string; `Layout.split` has tests for at least one case per `Constraint` variant plus a mixed-constraint case; 100% of `tui-core` public members have explicit result types per `SCALA_CODE_STYLE.md`. |
| 3. `tui-terminal` | `JLine3Backend` passes a manual smoke test (raw mode enters/exits cleanly, resize is detected, at least one key and one mouse event round-trip correctly) documented in the module's README; `Backend` trait has no JLine-specific leakage in its signatures (verifiable by confirming a second, trivial fake `Backend` can be written in a test without touching JLine types). |
| 4. `tui-runtime` | `hello-world` renders and redraws correctly on a real terminal; `HeadlessBackend` exists and a test using it renders the same output as an equivalent `JLine3Backend` run would (assert on `Buffer` contents, not a live terminal); `RenderThread` guard tests pass both with and without a registered render thread. |
| 5. Tier 0+1 widgets | Every widget has a render-to-`Buffer` test per `SPEC.md`/`PLAN.md` §9; `Layout`+widget composition covered by at least one test combining `Row`/`Column` with two Tier 1 widgets. |
| 6. `tui-dsl` v1 | `hello-world` example rewritten through the DSL produces byte-identical `Buffer` output to the immediate-mode version from step 4 (this is the concrete check that the DSL is a faithful layer over `tui-runtime`, not a divergent reimplementation). |
| 7. Tier 2 + `tui-macros` | `deriveForm`/`bindAction` (`SPEC.md` §6) have at least one test each demonstrating zero reflection use (e.g. run under a `SecurityManager`-style reflection-denial check, or simpler: grep-based CI check that `tui-macros` and its call sites contain no `java.lang.reflect` imports); focus/event-routing (`SPEC.md` §5.4) has tests for tab-order traversal and event-bubbling stop-propagation. |
| 8. `counter`, `todo-list` | Both pass the headless `Pilot`-equivalent end-to-end test (`PLAN.md` §9): press key → assert rendered buffer, for at least the primary interaction path of each. |
| 9. Tier 3 + `dashboard` | `dashboard` example runs with tick-rate animation for at least 60 seconds without a rendering artifact or memory growth (manual or scripted soak-test check — no automated leak-detection tooling is specified here, treat as an explicit manual verification step). |
| 10. Native-image | `hello-world` native-image binary builds and runs with correct output; expand to remaining `tui-examples`; if any example requires a reflect-config JSON to build, that's a signal the `tui-macros` discipline (§7) was violated somewhere upstream — fix the root cause rather than adding reflect-config as a workaround. |
| 11. Tier 4 + `form-demo` | `form-demo`'s validation exercises `Field[A].mapValidated` (`RESEARCH.md`, cue4s section) end-to-end, including at least one invalid-input case that surfaces a validation error in the rendered UI. |

## 12. Risk register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Reflection creeps into `tui-dsl`/widget code outside `tui-macros`, silently breaking native-image later | Medium — the path of least resistance for case-class introspection or event dispatch is often reflection | High — discovered late, expensive to retrofit (this is exactly the TamboUI lesson in `RESEARCH.md`) | Grep-based CI check for `java.lang.reflect`/`Class.forName` outside `tui-macros`; native-image build (step 10) run early enough in CI that violations surface before Tier 4, not just at the end |
| `CharWidth` has subtle Unicode bugs (e.g. incorrect handling of variation selectors, regional-indicator flag emoji) | Medium — Unicode width computation is a known hard problem even in mature libraries | Medium — visible as misaligned borders/text, not a crash, so may ship unnoticed | Explicit test matrix in §11 step 2; generate the width table from the Unicode Character Database rather than hand-rolling East-Asian-Width logic, and cross-check it against a well-tested implementation's output (permitted per §2.1's data-vs-code clarification — the table is standards-derived data, the lookup code stays original) |
| `Signal`/`Computed` dependency tracking has a correctness bug under conditional dependencies (the exact case Terminus's design handles, `RESEARCH.md`) | Low — the algorithm is adopted from working prior art, but the JVM reimplementation is new code | High — silent stale-UI bugs are hard to diagnose | Replicate the coverage categories Terminus's own `Reactive`/`Var`/`Computed` tests prove (conditional dependencies, unsubscribe-on-recompute, `peek` vs. `get` semantics — per §2.1's replicate-coverage-not-code clarification) as originally written tests, rather than inventing the test matrix from scratch without that reference |
| GraalVM native-image and JLine 3 interact poorly (terminal libraries are a common source of native-image friction beyond just reflection — e.g. JNI, `System.loadLibrary`) | Medium | High — could block §7/step 10 entirely | Validate with a minimal JLine 3 + native-image spike *before* step 10, ideally as early as step 3, so this is discovered while the terminal layer is still small and cheap to adjust |
| Scope creep toward Textual-level richness (CSS cascade, screen stack, async) | Medium — Textual's architecture is genuinely appealing and was researched in depth | Medium — dilutes v1 focus, delays a usable release | `SPEC.md` §7's non-goals table is the explicit guardrail; any PR/change adding scope from that table needs a deliberate decision recorded in `SPEC.md` §9, not a silent addition |
| `Style`/`Cell` allocation rate becomes a real render-loop bottleneck | Low-Medium — untested until real widgets exist | Medium — affects perceived responsiveness, not correctness | `SPEC.md` §9 flags this as an open decision; add a basic render-loop benchmark (JMH or a simple timed loop) once Tier 1 widgets exist (step 5), before optimizing anything |

## 13. CI and documentation

- **CI**: run `./mill __.test` and `./mill __.compile` on every push (this is a
  standalone repo, so "every change" means the whole repo, not a sub-path); add the
  reflection-grep check (§12) and, once step 10 lands, at least one native-image build
  job (can be slower/less frequent than the standard test job if build time is a
  concern — e.g. on a schedule or label-gated, not necessarily every push).
- **Per-module README**: each module directory (`core/`, `terminal/`, …) should
  have a short README (matching the convention visible in `worxbend`'s
  `libs/commons/describo/README.md`) covering what the module is for and a minimal
  usage example — do not let this substitute for real API docs (next bullet), it's
  an orientation doc.
- **Scaladoc**: public API members in `tui-core`, `tui-dsl` get Scaladoc comments
  focused on *why*/*contract* (pre/post-conditions, units, thread-safety), not
  restating the signature — no redundant comments, same principle
  `SCALA_CODE_STYLE.md` applies to code, applied here to API docs instead of inline
  code comments.
- **Example-driven docs**: `tui-examples` (§8) doubles as the primary "how do I use
  this" documentation — prefer keeping examples runnable and well-commented over
  writing a separate prose user guide for v1. Revisit if/when the library gets real
  external users beyond this repo.

## 14. Commit and checkpoint policy for autonomous execution

This section exists specifically for an AI coding agent looping over §10 unattended —
it answers "at what granularity do I commit" and "when do I stop and ask a human"
rather than leaving those to be improvised mid-run.

### 14.1 Commit granularity

One commit per numbered step in §10 (eleven steps → eleven commits for v1, roughly —
a step may split into more than one commit if it's naturally separable, e.g. step 5's
Tier 0 and Tier 1 widgets, but never fewer than one per step). A step's commit is only
made **after** its §11 acceptance-criteria row is satisfied and the full test suite
(`./mill __.test`) is green — not partway through, and not batched across multiple
steps into one commit. This keeps `git bisect` meaningful and gives each commit a
verifiable "done" state to point back to, matching §11's per-step criteria table.

Commit message convention: reference the step number and module(s) touched, e.g.
`step 4: tui-runtime render loop + headless backend`, so the commit history reads as
a log of §10 in order.

### 14.2 When to keep going vs. pause for human review

**Keep going autonomously** through:
- Any step whose §11 acceptance criteria are fully automatable (unit/render/headless
  tests) and pass.
- Normal edit-test-fix iteration within a step (failing tests are expected mid-step,
  not a stop condition by themselves).

**Pause and report to a human** at:
- **End of step 2** (`tui-core` frozen) — this is the one-way door `SPEC.md` §2 and §8
  flag: everything downstream depends on these shapes, so a human sanity-check before
  building Tier 0 widgets on top of it is cheap insurance against a costly later
  reshape.
- **End of step 3** (`tui-terminal`) — §11's acceptance criteria for this step
  explicitly require a "manual smoke test... documented in the module's README,"
  i.e. it names a check that cannot be fully automated (a real TTY, actual raw-mode
  behavior) — the agent should attempt it, document what it observed, and flag the
  result for human confirmation rather than self-certifying.
- **Before step 10** (native-image) — per the risk register (§12), validate the
  minimal JLine 3 + native-image spike as early as step 3 (not just at step 10) so any
  GraalVM/JLine incompatibility surfaces while it's still cheap to address; if that
  early spike fails, stop and report rather than continuing on the assumption it'll
  resolve itself by step 10.
- **Any acceptance-criteria row that fails 3 consecutive fix attempts** — a repeated
  failure past that point is a signal the step's design assumption (not just the
  implementation) may be wrong; report what was tried and why it didn't work rather
  than continuing to iterate blindly or silently descoping the criterion.
- **Any change that would add scope from `SPEC.md` §7's non-goals table** — per §12's
  "scope creep" risk, this always needs an explicit human decision, recorded back into
  `SPEC.md` §9, before proceeding — never a silent addition mid-loop.

### 14.3 Definition of "v1 done"

All eleven steps in §10 complete, all §11 acceptance criteria satisfied, `./mill
__.test` and `./mill __.compile` green, `hello-world` through `form-demo` (§8) all run
both on the JVM and as native-image binaries. At that point: bump every module's
`publishVersion` from `0.1.0-SNAPSHOT` to `0.1.0` together (§4.1's single-version
policy) and tag a release — this is the natural stopping point for the loop, not a
step to keep iterating past without a new plan.

Re-consult `RESEARCH.md` §"Synthesis" and `SPEC.md` §7 (non-goals) before any
architectural decision not already covered above — together they're the compressed
rationale for most choices in this plan.
