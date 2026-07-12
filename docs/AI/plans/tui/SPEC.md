# Technical Specification: Scala 3 TUI library

**Status**: draft. Companion to [`PLAN.md`](PLAN.md) (phased execution) and
[`RESEARCH.md`](RESEARCH.md) (prior-art analysis this spec draws its concrete shapes
from). Where this spec gives a type/trait signature, treat it as the target shape —
the implementing agent may adjust names during implementation but should flag any
structural deviation (e.g. adding a type parameter, changing a trait to a class) rather
than silently drifting from it, since downstream modules in `PLAN.md` §4 are specified
against these shapes.

Follow this repo's [`SCALA_CODE_STYLE.md`](../../../SCALA_CODE_STYLE.md) throughout —
this spec does not repeat general style rules (naming, error handling via `Either`
+ domain ADTs, no `return`, explicit result types on public members), only rules
specific to this library.

## 1. Package and module naming

- Root package: `io.worxbend.tui`, sub-packaged per module: `io.worxbend.tui.core`,
  `io.worxbend.tui.terminal`, `io.worxbend.tui.widgets`, `io.worxbend.tui.runtime`,
  `io.worxbend.tui.dsl`, `io.worxbend.tui.macros`.
- This is a standalone repository (`PLAN.md` §3), not nested inside `worxbend`'s
  `libs/`. Mill module *directories* live at the repo root and use short names
  (`core/`, `terminal/`, …, no `tui-` prefix — there's no sibling-module ambiguity to
  disambiguate against outside a monorepo). **Published artifact ids** keep the
  `tui-` prefix (`tui-core`, `tui-terminal`, …) via an explicit `artifactName`
  override per module — see `PLAN.md` §4.1 for the concrete `package.mill` shape.
  Package names never repeat the prefix either way (`io.worxbend.tui.core`, not
  `io.worxbend.tuicore`).
- One publishable artifact per Mill module, `publishVersion` synchronized across all
  `tui-*` modules (see §8, versioning) — do not let individual modules drift to
  independent version numbers in v1; that's a `PLAN.md`-out-of-scope concern (ratatui's
  per-crate stability gradient, `RESEARCH.md`, is about API *stability guarantees*, not
  independent version numbers — don't conflate the two).

## 2. `tui-core` — foundational types

Maximum-stability tier (ratatui's `-core` stability-gradient principle,
`RESEARCH.md`). Once `tui-widgets`/`tui-runtime`/`tui-dsl` exist, breaking changes here
ripple through everything — get these shapes right before building on top of them
(this is why `PLAN.md` §10 step 2 requires full test coverage before moving on).

### 2.1 Geometry

```scala
package io.worxbend.tui.core

final case class Rect(x: Int, y: Int, width: Int, height: Int):
  def area: Int = width * height
  def isEmpty: Boolean = width == 0 || height == 0
  def intersection(other: Rect): Rect
  def contains(pos: Position): Boolean

final case class Position(x: Int, y: Int)

final case class Size(width: Int, height: Int)
```

### 2.2 Cell, Buffer

```scala
final case class Cell(symbol: String, style: Style):
  def isBlank: Boolean = symbol.isEmpty || symbol == " "

object Cell:
  val Empty: Cell = Cell(" ", Style.Default)

final class Buffer(val area: Rect):
  private val cells: Array[Cell] = Array.fill(area.area)(Cell.Empty)

  def get(x: Int, y: Int): Cell
  def set(x: Int, y: Int, cell: Cell): Unit
  def setString(x: Int, y: Int, text: String, style: Style): Unit
  def diff(other: Buffer): Iterator[(Position, Cell)]  // changed cells only —
    // this is the method the terminal backend calls each frame to decide what
    // actually needs to be written (RESEARCH.md: "diff-based terminal updates",
    // ratatui + TamboUI both describe this exact mechanism)
```

`symbol: String` rather than `Char` on `Cell` is deliberate — a rendered terminal cell
can hold a multi-codepoint grapheme cluster (combining characters, some emoji). Storing
`Char` would make correct-by-construction impossible; storing `String` pushes the
correctness requirement onto `CharWidth` (§2.4) instead, which is where it belongs.

### 2.3 Style, Color

```scala
enum Color:
  case Reset, Black, Red, Green, Yellow, Blue, Magenta, Cyan, White
  case Rgb(r: Int, g: Int, b: Int)
  case Indexed(index: Int)  // 256-color palette

opaque type Modifiers = Int  // bitset: Bold, Dim, Italic, Underline, ...
object Modifiers:
  val None: Modifiers = 0
  val Bold: Modifiers = 1 << 0
  val Dim: Modifiers = 1 << 1
  // ...
  extension (m: Modifiers)
    def |(other: Modifiers): Modifiers = m | other
    def has(flag: Modifiers): Boolean = (m & flag) != 0

final case class Style(
    fg: Option[Color] = None,
    bg: Option[Color] = None,
    modifiers: Modifiers = Modifiers.None,
):
  def fg(color: Color): Style = copy(fg = Some(color))
  def bg(color: Color): Style = copy(bg = Some(color))
  def bold: Style = copy(modifiers = modifiers | Modifiers.Bold)
  // ... one such extension-flavored builder method per modifier, mirroring
  // TamboUI's `.bold().cyan()` chain (RESEARCH.md) but returning a new
  // immutable Style rather than mutating `this`

object Style:
  val Default: Style = Style()
```

`Modifiers` as an `opaque type Int` (bitset) rather than a `Set[Modifier]` is a
deliberate allocation-avoidance choice — `Style` values are created per-cell,
potentially thousands of times per frame, so keeping `Style` itself a small
value class with no boxed collection inside matters for render-loop throughput
(`PLAN.md` does not currently have a benchmarking milestone — see §9.2 "Still open"
below, this is flagged as a risk to watch, not a solved problem).

### 2.4 Text primitives

```scala
final case class Span(content: String, style: Style)
final case class Line(spans: Seq[Span]):
  def width: Int = spans.map(s => CharWidth.of(s.content)).sum
final case class Text(lines: Seq[Line])

object CharWidth:
  def of(text: String): Int                          // display width, not .length
  def substringByWidth(text: String, maxWidth: Int): String  // width-aware truncation
  def isWideCodePoint(codePoint: Int): Boolean        // CJK, fullwidth forms, etc.
```

Non-negotiable rule, carried directly from TamboUI's `AGENTS.md` (`RESEARCH.md`):
**no code outside `CharWidth` itself may call `String.length` or `String.substring`
for anything that affects layout or rendering.** Enforce this with a scalafix custom
rule or a grep-based CI check (`PLAN.md` §13 should add this check once `tui-core`'s
CharWidth lands — track as a follow-up if not folded in directly) rather
than relying on code review catching every instance.

### 2.5 Layout

```scala
enum Constraint:
  case Length(cells: Int)
  case Percentage(pct: Int)
  case Ratio(numerator: Int, denominator: Int)
  case Min(cells: Int)
  case Max(cells: Int)
  case Fill(weight: Int = 1)

object Constraint:
  // conveniences so call sites read like ratatui's macros without needing macros:
  given Conversion[Int, Constraint] = Length(_)
  given Conversion[Double, Constraint] = pct => Percentage((pct * 100).toInt)
  def fill: Constraint = Fill(1)

enum Direction:
  case Horizontal, Vertical

final case class Layout(direction: Direction, constraints: Seq[Constraint], spacing: Int = 0):
  def split(area: Rect): Seq[Rect]  // the constraint solver
```

The `Int`/`Double` `given Conversion`s are the Scala 3 answer to the "constraint
shorthand" goal in `PLAN.md` §5 (`Constraint` buildable "from a plain `Int`... `Double`
... or `Constraint.fill`") — implemented as implicit conversions at call sites that take
`Constraint`, not as overloaded factory methods per unit the way TamboUI does
(`.length(n)`, `.percent(n)` as separate static methods, `RESEARCH.md`). Keep the
conversions narrowly scoped (only where a `Constraint` is expected) to avoid the classic
implicit-conversion foot-gun of `Int`s silently becoming `Constraint`s somewhere
unrelated — put them in `Constraint`'s companion, not in a global `Predef`-style import.

### 2.6 Widget traits

```scala
trait Widget:
  def render(area: Rect, buffer: Buffer): Unit

trait StatefulWidget[S]:
  def render(area: Rect, buffer: Buffer, state: S): Unit
```

Per `RESEARCH.md`'s TamboUI Widget-trait comparison: this is intentionally the
TamboUI shape, not the ratatui shape (`fn render(self, ...)`, by-value receiver) —
on the JVM there is no equivalent "moved vs. borrowed" distinction to encode, so
ratatui's `WidgetRef`/by-reference workaround (added specifically to let a widget be
stored and rendered more than once) has no analog to build here. A plain Scala
`trait Widget` instance can already be rendered any number of times.

## 3. `tui-terminal` — backend abstraction

```scala
package io.worxbend.tui.terminal

trait Backend:
  def size: Either[BackendError, Size]
  def draw(buffer: Buffer): Either[BackendError, Unit]
  def enableRawMode(): Either[BackendError, Unit]
  def disableRawMode(): Either[BackendError, Unit]
  def enterAlternateScreen(): Either[BackendError, Unit]
  def leaveAlternateScreen(): Either[BackendError, Unit]
  def readEvent(timeout: Duration): Either[BackendError, Option[Event]]
  def close(): Unit

enum BackendError:
  case Io(cause: Throwable)
  case UnsupportedTerminal(reason: String)
  case NotInRawMode

enum Event:
  case Key(code: KeyCode, modifiers: KeyModifiers)
  case Mouse(x: Int, y: Int, kind: MouseEventKind, modifiers: KeyModifiers)
  case Resize(size: Size)
  case Tick  // synthetic, emitted at the runner's configured tick rate — not a raw
             // terminal event, injected by tui-runtime (see §4); listed here because
             // it shares the Event ADT consumers pattern-match against
```

v1 ships exactly one `Backend` implementation, `JLine3Backend` (per `PLAN.md` §4's
rationale: JLine 3 is the JVM's most mature terminal library, and it's TamboUI's
default too), pinned to `mvn"org.jline:jline:3.30.13"` — the latest JLine 3.x patch
release as of this spec's writing (2026-07). **Do not jump to the JLine 4.x major
line for v1**: 4.x was released 2026-05 and is too new to be a safe default for a
library that itself wants to be a stable dependency; revisit once 4.x has had time to
mature, tracked as a documented follow-up rather than an open blocker. Bump to the
latest 3.30.x patch (not a new minor/major) at implementation kickoff if a newer patch
exists. See `PLAN.md` §4.1 for the exact `package.mill` dependency declaration.

The `Backend` trait exists from day one specifically so a `HeadlessBackend` (for the
`PLAN.md` §9 testing harness, adapted from Textual's `headless_driver.py`) is a second
implementation, not a special case bolted onto `JLine3Backend` — build both behind the
same trait before either accretes backend-specific leakage into `tui-runtime`.

`Either[BackendError, A]` rather than throwing, per this repo's
[error-handling convention](../../../SCALA_CODE_STYLE.md#error-handling): backend I/O
failures are recoverable-by-the-caller in the sense that an app can choose to
degrade (log and continue vs. abort), so they belong in the return type, not as
exceptions. Reserve actual `throw` for genuine defects (e.g. calling `draw` before
`enableRawMode`, if that's made an invariant rather than a modeled error).

## 4. `tui-runtime` — event loop, reactive state, render-thread model

### 4.1 Reactive state primitive

Adopted near-verbatim from Terminus's fine-grained signals system
(`RESEARCH.md`, Terminus section — read that section before touching this one, it
explains *why* this shape and not a React/Elm dispatch-and-diff model):

```scala
package io.worxbend.tui.runtime

sealed trait Reactive[A]:
  def peek: A
  def get(using ctx: ReactiveScope): A
  def map[B](f: A => B): Reactive[B]

final class Signal[A] private (initial: A) extends Reactive[A]:
  def set(value: A): Unit
  def update(f: A => A): Unit
  // set/update mark subscribers stale; they do not recompute anything eagerly
object Signal:
  def apply[A](initial: A): Signal[A]

final class Computed[A] private (thunk: ReactiveScope ?=> A) extends Reactive[A]
object Computed:
  def apply[A](thunk: ReactiveScope ?=> A): Computed[A]

trait ReactiveScope:
  private[runtime] val stack: mutable.Stack[Subscriber]
```

Naming deviates slightly from Terminus's own names (`Var` → `Signal`, `React` →
`ReactiveScope`) to avoid colliding with the unrelated, better-known meaning of `Var`
in Scala (`var` the keyword) and to make the capability's purpose self-describing at
call sites (`(using scope: ReactiveScope)` reads clearly; `(using ctx: React)` does
not, out of context). The mechanics — lazy stale-marking on `set`, dependency
re-establishment via a scope stack pushed during `Computed` recomputation, untracked
reads via `.peek` — are unchanged from Terminus; see `RESEARCH.md` for the full
mechanics explanation before reimplementing this.

**Thread-safety requirement not present in Terminus's current code** (their `ui`
module has no render-thread concept yet): `Signal.set` must be safe to call from
event-handler code running on the render thread (the common case) and must document
that calling it from any other thread is a defect, matching TamboUI's render-thread
contract (§4.2). Do not add cross-thread synchronization to `Signal`/`Computed`
themselves (that would tax the common single-threaded case) — instead enforce via
`RenderThread.checkRenderThread()` assertions at the `Signal.set` entry point, same
mechanism as §4.2.

### 4.2 Render-thread model

Adopted from TamboUI (`RESEARCH.md`):

```scala
object RenderThread:
  def isRenderThread: Boolean
  def checkRenderThread(): Unit  // throws IllegalStateException if violated —
                                  // this is a defect-detection assertion, not a
                                  // recoverable error, hence throw not Either
  def runOnRenderThread(body: => Unit): Unit  // runs inline if already on it
  def runLater(body: => Unit): Unit           // always queued
```

Guard is a no-op when no render thread is registered (TamboUI's exact behavior,
`RESEARCH.md`), specifically so unit tests of widgets/`Signal` usage don't need a
running runtime — this is why `PLAN.md` §9 can specify `tui-runtime` unit tests as
running "without a real terminal."

### 4.3 Runner

```scala
final case class RunnerConfig(
    tickRate: Option[Duration] = None,
    mouseCapture: Boolean = false,
)

trait Runner:
  def run(
      handleEvent: (Event, RunnerHandle) => Boolean,  // returns "should redraw"
      render: Frame => Unit,
  ): Either[RunnerError, Unit]

trait RunnerHandle:
  def quit(): Unit
  def runOnRenderThread(body: => Unit): Unit

final class Frame(val area: Rect, private[runtime] val buffer: Buffer):
  def renderWidget(widget: Widget, area: Rect): Unit
  def renderStatefulWidget[S](widget: StatefulWidget[S], area: Rect, state: S): Unit
```

This is the direct Scala analog of TamboUI's `TuiRunner` (`RESEARCH.md`) — the
mid-level API tier from `PLAN.md` §5's three-tier design. `tui-dsl` is built *on top
of* `Runner`, not as an alternative to it — the DSL's `TuiApp` (§5) owns a `Runner`
internally and drives it from `Signal`/`Computed` invalidation rather than exposing
the raw `(Event, RunnerHandle) => Boolean` callback to DSL users.

## 5. `tui-dsl` — declarative API

### 5.1 `Element` — the retained-mode tree

```scala
package io.worxbend.tui.dsl

sealed trait Element:
  def style: Style
  def widget: Widget  // every Element ultimately renders through a tui-core Widget

object Element:
  def text(content: String): TextElement
  def panel(title: String)(children: Element*): PanelElement
  def row(children: Element*): RowElement
  def column(children: Element*): ColumnElement
  def spacer: Element
  // ... one factory per widget in the PLAN.md §6 backlog, mirroring TamboUI's
  // Toolkit static-import factory set (RESEARCH.md) but as top-level `export`ed
  // defs from this object rather than requiring `import Toolkit.*`-style
  // static-import boilerplate — see §5.3 on import ergonomics
```

### 5.2 Styling and layout extension methods

```scala
extension (e: Element)
  def bold: Element
  def dim: Element
  def color(c: Color): Element
  def rounded: Element        // border style shorthand
  def onKeyEvent(handler: KeyEvent => Boolean): Element
  def onMouseEvent(handler: MouseEvent => Boolean): Element

extension (e: Element)
  def length(cells: Int): Element      // wraps as a layout item with this Constraint
  def percent(pct: Int): Element
  def fill: Element
```

Chained calls return a new `Element` (immutable), matching TamboUI's fluent style
(`RESEARCH.md`) but via `extension` methods rather than instance methods on a builder
class — this keeps `Element` itself a plain sealed hierarchy (pattern-matchable,
useful for the `tui-dsl` construction tests in `PLAN.md` §9) while still supporting
the fluent chain syntax at call sites.

### 5.3 `TuiApp`

```scala
trait TuiApp:
  def view(using ReactiveScope): Element
  def config: RunnerConfig = RunnerConfig()

  final def run(): Either[RunnerError, Unit] =
    // owns a Runner + ReactiveScope; on any Signal invalidation reachable from
    // the last `view` evaluation, triggers a redraw; on KeyEvent/MouseEvent,
    // routes to the Element tree's onKeyEvent/onMouseEvent handlers by
    // hit-testing the last-rendered layout (see §5.4, focus/routing) before
    // falling through to unhandled-event bubbling
```

Matches the target ergonomics sketched in `PLAN.md` §5's `HelloWorld` example.
Note `view` takes a `ReactiveScope` capability rather than an explicit `state:
State` parameter as originally sketched in `PLAN.md` §5 — this supersedes that
sketch now that §4.1 has settled on Terminus's signals model rather than an
explicit-state-passing model; **`PLAN.md` should be read with this correction**
(cross-referenced from `PLAN.md` directly, see the update noted there).

### 5.4 Focus and event routing

Not fully specified yet — deliberately left open pending Tier 2 implementation
(`PLAN.md` §10 step 7). Requirements, drawn from `RESEARCH.md`:

- Tab-order focus traversal and click-to-focus, matching TamboUI's Toolkit
  (`RESEARCH.md`, "Automatic focus management").
- Event *bubbling* up the `Element` tree from the hit-tested leaf, matching
  Textual's `MessagePump` bubbling model (`RESEARCH.md`, Textual section) —
  **adopt the bubbling behavior, not the asyncio message-bus machinery it's
  implemented with in Textual.** A handler returning `true` from
  `onKeyEvent`/`onMouseEvent` stops propagation (consumes the event); returning
  `false` lets it bubble to the parent `Element`.
- Do not build a `Screen` stack (Textual's modal/multi-screen navigation model,
  `RESEARCH.md`) for v1 — dialogs are `Element`s drawn last within a single `view`,
  matching TamboUI's `DialogElement` approach (`PLAN.md` §6, Tier 4).

## 6. `tui-macros` — compile-time codegen

Scope, precisely: anywhere `tui-dsl` needs to bridge *user-defined* code (a case
class, a method reference) into the framework's dispatch machinery, that bridge must
be generated at compile time via Scala 3 `inline`/macros, never via runtime
reflection. This is the single hard constraint carried from TamboUI's
`@OnAction`/annotation-processor pattern (`RESEARCH.md` — "the single most important
lesson to carry into the Scala design").

Two concrete v1 use cases (both land in `PLAN.md` §10 step 7, alongside Tier 2
widgets):

```scala
package io.worxbend.tui.macros

// 1. Case-class -> Form field derivation
inline def deriveForm[A]: FormSpec[A]  // inline def, expands via Mirror.ProductOf[A]
                                        // at the call site — no runtime reflection,
                                        // no annotation processor step (Scala 3's
                                        // built-in `Mirror` derivation replaces the
                                        // need for TamboUI's separate
                                        // tamboui-annotations + tamboui-processor
                                        // module pair entirely)

// 2. Compile-time event-handler binding, replacing @OnAction
inline def bindAction[A](inline handler: A => Unit): ActionHandler[A]
```

Note this is a real simplification opportunity vs. TamboUI: Scala 3's `Mirror`
typeclass-derivation mechanism (stdlib, not a macro library) covers the
case-class-introspection use case TamboUI needed a *separate annotation processor
module* for, because Java has no compile-time reflection-free equivalent. **Do not
port TamboUI's two-module (`tamboui-annotations` + `tamboui-processor`) split** —
`tui-macros` should be one module, and `deriveForm` should prefer `Mirror`-based
`inline given`/`inline def` derivation over hand-written macros wherever `Mirror`
alone is sufficient, falling back to full `scala.quoted` macros only where `Mirror`
genuinely can't express what's needed (e.g. capturing source positions for error
messages, which `bindAction` may need for good compile errors on a malformed handler).

## 7. Non-goals for v1 (explicit)

Carried from `RESEARCH.md`'s Textual section — each of these was evaluated and
deliberately excluded, not overlooked:

| Feature | Why excluded | Reference |
|---|---|---|
| Full CSS cascade/selector engine | Textual-scale investment (`RESEARCH.md`); fluent Scala 3 extension methods (§5.2) cover styling ergonomics without a parser/cascade-resolution subsystem | RESEARCH.md, Textual §css |
| Asyncio-style message bus / mandatory async runtime | Conflicts with the effect-agnostic-core principle (cue4s, `RESEARCH.md`); event bubbling (§5.4) is adopted without the async coupling it comes bundled with in Textual | RESEARCH.md, cue4s + Textual |
| Screen stack / multi-screen navigation | Heavier than the widget backlog requires; TamboUI's dialog-as-widget approach suffices | RESEARCH.md, Textual §screen.py |
| Multi-protocol image widget (Kitty/iTerm/Sixel) | Highest platform-compatibility risk, lowest priority — `PLAN.md` §6 Tier 4 | RESEARCH.md, TamboUI |
| `DataTable`/`DirectoryTree`/syntax-highlighted `TextArea` | Meaningfully harder than the rest of the backlog; `PLAN.md` §6 Tier 5, explicit opt-in only | RESEARCH.md, Textual |
| Scala.js / web target | Terminus/cue4s precedent exists but was never requested; would pull `tui-terminal` toward a `shared`/`jvm`/`js`/`native` split prematurely | — |

## 8. Versioning and API stability

Adopt ratatui's stability-gradient principle (`RESEARCH.md`) but expressed as
documentation/discipline, not tooling, for v1 (MiMa-style binary-compatibility
checking is a reasonable follow-up once the API has actually shipped a 0.1, not
before):

- `tui-core`: treat as frozen once `PLAN.md` §10 step 2 completes and Tier 0–1 widgets
  are built on top of it (step 5) without needing changes. Breaking changes here after
  that point require a version bump and a changelog entry, not a silent edit.
- `tui-widgets`, `tui-runtime`: expected to evolve per-tier as the backlog fills in;
  breaking changes acceptable pre-1.0.
- `tui-dsl`: least stable of the published modules pre-1.0 — this is where API
  ergonomics get tuned based on how the `tui-examples` apps actually feel to write.
- `tui-macros`: internal-facing; not intended for direct external use in v1 (no
  external `Mirror`/macro extension points published yet) — keep its public surface
  minimal (`deriveForm`, `bindAction`, and whatever their result types are) rather than
  exposing macro internals.
- Single `publishVersion` across all `tui-*` modules for v1 (§1) — split into
  independent per-module versioning only if/when there's a concrete reason (e.g. a
  widget-library ecosystem growing around `tui-core` the way ratatui's did).

## 9. Decisions log

### 9.1 Resolved (do not re-litigate without a concrete new fact)

1. **GraalVM native-image vs. Scala Native** — v1 targets GraalVM native-image per
   the original request. **Resolved further**: use Mill's built-in `NativeImageModule`
   trait (verified against mill-build.org's own docs, not a third-party plugin) —
   see `PLAN.md` §7 for the concrete `package.mill` shape (`jvmVersion`,
   `nativeImageOptions`, the `nativeImage` task). Scala Native remains a documented
   alternative/stretch goal, not a live fork to weigh mid-implementation. Only revisit
   if native-image reflection friction turns out worse than expected despite the
   `tui-macros` discipline in §6 above, and only with a concrete measured problem in
   hand, not speculatively.
2. **Repository layout** — standalone repo, not nested in `worxbend`'s `libs/`;
   `worxbend`-style conventions (package naming, `SbtModule`+`PublishModule`, Mill,
   ScalaTest) carried over but module directories sit at the repo root
   (`core/`, `terminal/`, …) rather than under `libs/tui/`. Published artifact ids
   keep the `tui-` prefix regardless. See `PLAN.md` §3–§4.1 for the full rationale and
   a concrete, verified `moduleDeps`/`package.mill` example (this repo, `worxbend`,
   had no prior example to copy from, so one was written and checked against Mill's
   own documentation rather than left for the implementing agent to guess).
3. **JLine version** — pinned to `mvn"org.jline:jline:3.30.13"` (§3 above); do not
   move to the JLine 4.x major line for v1, it's too new (released 2026-05) to be a
   safe default dependency yet.

### 9.2 Still open — the implementing agent should make an explicit call and record
it back into this file rather than defaulting silently

1. **Style allocation strategy** (§2.3) — `Style` as an immutable case class is
   simplest and matches TamboUI/ratatui's model, but if profiling
   (no benchmarking milestone currently exists in `PLAN.md` — add one before
   optimizing) shows per-cell `Style` allocation is a hot path, consider a
   flyweight/interning `StyleId` scheme instead. Do not preemptively optimize this
   without a measurement.
2. **`Buffer` mutability** (§2.2) — specified as a mutable `Array[Cell]`-backed class
   for render-loop performance (matching every reference implementation researched —
   none of the four use a persistent/immutable buffer). Confirm this doesn't conflict
   with the effect-agnostic-core goal (§7) — it doesn't, mutability here is an
   implementation detail behind `Widget.render(area, buffer)`, not part of the public
   effect-purity surface.
3. **Focus/event-routing algorithm details** (§5.4) — intentionally left as
   requirements rather than a full spec; work out the concrete hit-testing/traversal
   algorithm during `PLAN.md` §10 step 7 and fold the result back into this section.
