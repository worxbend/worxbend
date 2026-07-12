# Research: existing TUI libraries

## Research method

Reference implementations were cloned shallow (`git clone --depth 1`) into a scratch
directory outside the repo and inspected directly (READMEs, architecture docs, module
trees, and representative source files). Re-run the same clones before resuming this
work if deeper source inspection is needed — nothing from the clones is vendored here.

```bash
git clone --depth 1 https://github.com/ratatui/ratatui.git
git clone --depth 1 https://github.com/tamboui/tamboui.git
git clone --depth 1 https://github.com/creativescala/terminus.git
git clone --depth 1 https://github.com/neandertech/cue4s.git
git clone --depth 1 https://github.com/Textualize/textual.git
```

Useful starting points in each repo: ratatui's `ARCHITECTURE.md`, TamboUI's
`AGENTS.md` + `README.md`, Terminus's `core/shared` + `ui/shared` source trees,
cue4s's `README.md` + `modules/core`, Textual's `src/textual/{reactive,message,
message_pump,dom,css,drivers,pilot}.py`.

---

## ratatui (Rust) — the reference paradigm

- **Model**: immediate-mode rendering. The whole UI is redrawn to an in-memory
  `Buffer` every frame; the terminal backend diffs the buffer against the previous
  frame and only writes the changed cells.
- **Workspace layout** (post-0.30 restructuring, see `ARCHITECTURE.md`): split from one
  monolithic crate into a Cargo workspace:
  - `ratatui-core` — foundational types only: `Widget`/`StatefulWidget` traits, `Buffer`,
    `Rect`, `Text`/`Line`/`Span`, `Style`, layout/`Constraint` system. Designed for
    maximum API stability so third-party widget crates can depend on it without churn.
  - `ratatui-widgets` — all built-in widgets (`Block`, `Paragraph`, `List`, `Table`,
    `Chart`, `Gauge`, `Sparkline`, …), depends only on `-core`.
  - Backend crates (`ratatui-crossterm`, `ratatui-termion`, `ratatui-termina`,
    `ratatui-termwiz`) — each an independent, swappable terminal I/O backend, depends
    only on `-core`.
  - `ratatui-macros` — declarative macros to cut boilerplate (`layout!`, `constraints!`).
  - `ratatui` (facade) — re-exports everything above for the common case; this is the
    crate application authors normally depend on directly.
- **Design principle worth stealing**: the stability gradient. Core types are frozen and
  conservative; the facade crate is allowed to evolve fast. This lets third-party widget
  authors depend on the stable core without being coupled to fast-moving convenience APIs.
- **Layout**: constraint solver (`Constraint::{Length, Percentage, Ratio, Min, Max, Fill}`)
  driving a `Layout` that splits a `Rect` into sub-`Rect`s — this exact vocabulary is
  echoed almost 1:1 in both TamboUI and Terminus, so it's a de facto standard worth
  reusing rather than reinventing.
- **`Widget` trait, exact shape** (`ratatui-core/src/widgets/widget.rs`):
  ```rust
  pub trait Widget {
      fn render(self, area: Rect, buf: &mut Buffer);
  }
  ```
  Note the by-value `self` — ratatui's docs (quoted in-source) explicitly recommend
  implementing `Widget for &MyWidget` (reference receiver) rather than consuming the
  widget, specifically so a widget value can be stored and rendered more than once, and
  so heterogeneous widget collections can be rendered through a boxed trait object
  (`WidgetRef`, added 0.26 as an "unstable" trait for exactly this reason). This
  render-by-reference-not-by-value distinction is a real design decision the Scala
  translation has to make explicitly (a Scala `trait Widget { def render(area: Rect, buf: Buffer): Unit }`
  called on an instance is "by reference" by default, so this concern mostly
  disappears in Scala — worth noting in `SPEC.md` as a place where the JVM's reference
  semantics make a Rust-specific wrinkle moot).
- **Macros crate**: `ratatui-macros` ships `layout!`, `constraints!`, `horizontal!`,
  `vertical!`, `buffer_set!` — declarative macros purely to cut boilerplate around
  constructing `Layout`/`Constraint`/`Buffer` values tersely. Scala 3 doesn't need an
  equivalent for this specific case (`Layout(direction, Seq(Constraint...))` is already
  terse, and inline vararg `apply` methods cover it) — `tui-macros` in `PLAN.md` should
  stay scoped to compile-time codegen (event dispatch, form derivation) rather than
  syntax-sugar macros; note this explicitly so the plan doesn't accidentally grow a
  `ratatui-macros`-equivalent nobody asked for.
- **Widget catalog** (`ratatui-widgets` crate, for cross-reference against TamboUI's):
  `Block`/`Borders`, `Paragraph`, `List`, `Table`, `Tabs`, `Gauge`, `Sparkline`,
  `BarChart`, `Chart`, `Canvas`, `Calendar`, `Scrollbar`, `Clear` (blanks an area,
  useful for popups/dialogs over existing content), `Logo`/`Mascot` (project branding
  widgets — a fun, low-cost addition once the core set exists). Near-identical to
  TamboUI's catalog, confirming it as the real cross-ecosystem baseline widget set.

## TamboUI (Java) — closest analog to what we're building

TamboUI is explicitly "ratatui/bubbletea for the JVM," experimental, GraalVM-native-image
first-class. It is the most directly relevant prior art since it targets the same runtime
constraints (JVM host language, native-image compilation, JLine-class terminal backends)
that a Scala 3 library would inherit. **Read `AGENTS.md` in the TamboUI repo closely —
it is written as an agent-facing architecture brief and is a good template for the
`AGENTS.md`/contributor doc we should end up with.**

### Module structure (Gradle multi-module, mirror for our Mill layout)

| Module | Purpose |
|---|---|
| `tamboui-core` | `Buffer`, `Cell`, `Rect`, `Style`, `Layout`, `Text` primitives, `Widget`/`StatefulWidget` interfaces |
| `tamboui-widgets` | all widget implementations |
| `tamboui-jline3-backend`, `tamboui-aesh-backend`, `tamboui-panama-backend` | swappable terminal backends |
| `tamboui-tui` | mid-level framework: `TuiRunner`, event loop, key bindings, action handlers |
| `tamboui-toolkit` | high-level fluent/declarative DSL: retained-mode `Element` tree, focus management, event routing, drag support |
| `tamboui-css` | CSS-like styling (TCSS): selectors, cascade resolution, theme switching |
| `tamboui-annotations` + `tamboui-processor` | `@OnAction` annotation + compile-time annotation processor generating action-handler dispatch (avoids reflection — relevant for native-image, which is reflection-hostile) |
| `tamboui-image` | image rendering across multiple terminal protocols (Kitty, iTerm, Sixel, half-block, braille) |
| `tamboui-markdown` / `tamboui-toolkit-markdown` | Markdown rendering widget |
| `tamboui-picocli` | CLI-argument-parsing integration |
| `tamboui-benchmarks`, `tamboui-demos` | JMH benchmarks + runnable demo apps (also double as native-image compile targets) |

### Three API layers (a pattern to replicate)

1. **Immediate mode** (lowest level): construct a `Terminal<Backend>`, call
   `terminal.draw(frame -> widget.render(frame.area(), frame.buffer()))` yourself. Full
   control, most boilerplate.
2. **Mid-level (`TuiRunner`)**: owns the event loop. You pass an
   `(event, runner) -> shouldRedraw` handler and a `frame -> render(...)` function;
   `TuiRunner` handles raw-mode setup/teardown, tick-rate-driven redraws, resize events.
3. **High-level declarative Toolkit DSL** (closest to what the user wants us to emulate
   from Compose): retained-mode `Element` tree built with static-imported factory
   functions and fluent modifiers:

   ```java
   import static dev.tamboui.toolkit.Toolkit.*;

   class HelloDsl extends ToolkitApp {
     protected Element render() {
       return panel("Hello",
         text("Welcome to TamboUI DSL!").bold().cyan(),
         spacer(),
         text("Press 'q' to quit").dim()
       ).rounded();
     }
   }
   ```

   Notable Toolkit properties:
   - Elements compose via plain function calls + fluent chains (`text(...).bold().cyan()`),
     not inheritance.
   - Layout constraints expressed the same way as widget styling: `.length(n)`,
     `.percent(n)`, `.fill()`, `.min(n)`, `.max(n)`.
   - Automatic focus management (tab order, click-to-focus) and per-component event
     handlers (`.onKeyEvent()`, `.onMouseEvent()`) — the framework routes events rather
     than the app polling for them.
   - `@OnAction`-annotated component methods + generated dispatch, so most apps never
     touch a raw event `switch` at all.

### Widget catalog (use as the v1 widget backlog)

`Block`/border, `Paragraph`, `Text`, `List`, `Table`, `Tabs`, `Gauge`, `LineGauge`,
`Sparkline`, `DualSparkline`, `BarChart`, `Chart` (line/scatter), `Canvas` + shape
primitives, `Calendar`, `Scrollbar`, `TextInput`, `Checkbox`, `Toggle`, `Select`,
`Tree`, `Form`, `Spinner`, `WaveText`, `Braille` drawing, `Dialog`, image widget
(multi-protocol), Markdown view.

### `Widget`/`StatefulWidget`, exact shape (`tamboui-core`)

```java
@FunctionalInterface
public interface Widget {
    void render(Rect area, Buffer buffer);
}

public interface StatefulWidget<S> {
    void render(Rect area, Buffer buffer, S state);
}
```

Deliberately minimal — `Widget` is a `@FunctionalInterface`, so any lambda
`(area, buffer) -> {...}` is a valid widget with zero ceremony, and `StatefulWidget<S>`
is a plain generic interface, no inheritance hierarchy. This is the cleanest of the
four reference APIs at the trait-definition level and translates almost verbatim to
Scala 3:

```scala
trait Widget:
  def render(area: Rect, buffer: Buffer): Unit

trait StatefulWidget[S]:
  def render(area: Rect, buffer: Buffer, state: S): Unit
```

with the bonus that Scala 3's SAM (single-abstract-method) conversion gives `Widget`
lambda-literal support for free, matching Java's `@FunctionalInterface` ergonomics
without an annotation. See `SPEC.md` §2.6 for the finalized version of these traits —
adopted essentially as-is; note `SPEC.md` deliberately does **not** add an analog of
ratatui's `WidgetRef` (a JVM widget instance can already be stored and rendered any
number of times, so the Rust by-value/by-reference wrinkle that motivated `WidgetRef`
has nothing to solve here).

### Rendering & threading model

- Stateless widgets: `Widget.render(Rect, Buffer)`. Stateful widgets:
  `StatefulWidget<S>.render(Rect, Buffer, S)`.
- Single dedicated **render thread**, modeled after JavaFX/Swing: all UI state mutation
  must happen on it. `RenderThread.isRenderThread()` / `.checkRenderThread()` guard it;
  `runOnRenderThread()` / `runLater()` marshal work onto it. Enforcement is a no-op when
  no render thread is registered, so unit tests don't need special setup.
- JFR (Java Flight Recorder) events emitted for draw duration, event routing, focus
  changes, drag state — worth carrying over as a pattern for Scala (e.g. minimal,
  opt-in tracing hooks) even if the exact JFR mechanism doesn't map 1:1.

### GraalVM native-image

- First-class target: demos compile via `./gradlew :demos:<name>:nativeCompile`, and
  `run-demo.sh <name> --native` runs the compiled binary directly.
- The annotation-processor pattern for `@OnAction` (compile-time codegen instead of
  runtime reflection) exists specifically to keep the framework reflection-free, which
  is the main source of native-image friction (reflection config, unreachable-code
  warnings, slower/larger builds). **This is the single most important lesson to carry
  into the Scala design**: prefer compile-time metaprogramming (Scala 3 macros / inline)
  over runtime reflection anywhere the framework needs to bridge user code (e.g. case
  class → form field derivation, event-handler dispatch).

### Unicode/display-width discipline

TamboUI is emphatic (`AGENTS.md`) about never using `String.length()` or
`String.substring()` for terminal display math — Java string length is UTF-16 code
units, not terminal columns (e.g. CJK chars are double-width, emoji vary). It ships a
dedicated `CharWidth` utility (`CharWidth.of(text)`, `CharWidth.substringByWidth(...)`).
Scala inherits the exact same JVM string representation problem, so this needs its own
first-class utility in `core` from day one, not a widget-level patch.

## Terminus (Scala 3, creativescala) — closest same-language prior art

- **Scope**: currently lower-level than TamboUI/ratatui — "terminal interaction for
  Scala 3," i.e. raw terminal control (ANSI codes, raw mode, cursor, key reading) plus
  an early `ui` module, not yet a full widget ecosystem. Good source for the
  **terminal-backend layer**, less so for the widget layer.
- **Cross-build**: uses `sbt-crossproject` to target **JVM, Scala.js, and Scala Native**
  from one `core`/`ui` source tree (`shared`/`jvm`/`js`/`native` split). This is a real
  alternative to GraalVM native-image for a native binary target — Scala Native produces
  a native binary directly from Scala without needing GraalVM at all. **Resolved** in
  favor of GraalVM native-image for v1, via Mill's built-in `NativeImageModule` trait —
  see `SPEC.md` §9.1 and `PLAN.md` §7 for the concrete decision and build wiring.
- **`core` module** (shared): `AnsiCodes`, `RawMode`, `ApplicationMode`,
  `AlternateScreenMode`, `Cursor`, `Erase`, `Scroll`, `Color`, `Key`/`KeyCode`/
  `KeyModifier`, `KeyReader`/`TerminalKeyReader`, `NonBlockingReader`, `Reader`/`Writer`,
  `Format`. This is a clean, minimal terminal-control vocabulary — a good starting point
  for our own `core` terminal-I/O layer, adapted to the JVM (no need for the JS/Native
  split unless we deliberately choose multi-target later).
- **`ui` module** (shared): named after a React-ish component model —
  `Component`, `ComponentState`, `React` / `DefaultReact`, `Reactive`, `Submit` /
  `DefaultSubmit`, `Event` / `DefaultEvent`, `Listener`, `Focus` / `FocusId`,
  `Layout` / `DefaultLayout` / `LayoutProps`, `Constraint`, `Box` / `BoxProps`,
  `Row` / `Column`, `Text` / `TextProps` / `TextStyle`, `TextInput` / `TextInputProps`,
  `Select` / `SelectProps`, `Button` / `ButtonProps`, `Rule`, `Border`, `Style`, `Align`,
  `Justify`, `Insets`, `Buffer` / `CellArrayBuffer` / `ViewBuffer`, `Runtime`.
  Our target API ("Compose-like, but a proper Scala 3 DSL") sits between Terminus and
  TamboUI: retained-mode + reactive like Terminus, ergonomic and declarative like
  TamboUI's Toolkit and JetBrains Compose.

  **This is not React/Elm — it's a fine-grained signals system**, close to
  SolidJS/Preact-Signals or Reactively, and the exact mechanism is worth internalizing
  because it's the strongest concrete input into `tui-runtime`'s state model
  (`SPEC.md` §4). The core types, from `terminus.ui.react.Reactive` and
  `terminus.ui.capability.React`:

  ```scala
  sealed trait Reactive[A]:
    def peek: A                    // read without subscribing
    def get(using ctx: React): A   // read + subscribe the enclosing context
    def map[B](f: A => B): Reactive[B]

  final class Var[A](private var currentValue: A) extends Reactive[A]:
    def set(newValue: A): Unit         // notifies subscribers, marks them stale
    def update(f: A => A): Unit

  final class Computed[A](thunk: React ?=> A) extends Reactive[A], Listener:
    // recomputes lazily on next get/peek once marked stale; unsubscribes from
    // its old dependency set and re-subscribes on every recompute (so
    // conditional dependencies — `if cond.get then a.get else b.get` — are
    // handled correctly: whichever branch didn't run this time is dropped)

  trait React:                    // a context capability, not a widget concept
    val stack: mutable.Stack[Listener]  // tracks "which Computed/Component is
                                         // currently evaluating," so a `Var.get`
                                         // call knows who to subscribe
  ```

  Mechanics: `Var.set` doesn't recompute anything eagerly — it just flips subscribers
  to `State.Stale` (a dirty flag). Recomputation happens lazily, on the next `get`/
  `peek` of a `Computed` that was marked stale, by re-running its `thunk` with the
  `React` capability's stack pushed so any `Var`/`Computed` reads *during that specific
  run* re-establish the dependency edges. This gives **automatic, dynamic dependency
  tracking** (no manual dependency arrays, unlike React's `useEffect(fn, [deps])`) and
  supports conditional/branching dependencies correctly by construction, at the cost of
  requiring a `React` capability (`given`/`using`) to be threaded through anywhere a
  signal is read reactively — enforced at compile time via
  `@implicitNotFound` with a message pointing the user at `.peek` for untracked reads.

  **Why this matters for our design** (`SPEC.md` §4, `PLAN.md` §4): this is a working,
  idiomatic-Scala-3 (`given`/`using`, opaque capability trait, sealed hierarchy)
  implementation of exactly the "how does state change trigger re-render" problem that
  Textual's `Reactive[T]` descriptor (RESEARCH.md, Textual section) solves dynamically
  via `watch_<name>`/`compute_<name>` naming conventions. Terminus's version is the
  more Scala-idiomatic of the two — no string-based dunder dispatch, dependency tracking
  is structural (the `React` stack) rather than name-based reflection, and it's already
  proven to compile and type-check as real Scala 3. **Recommendation carried into
  `SPEC.md`: base `tui-runtime`'s reactive state primitive directly on this `Var`/
  `Computed`/`React`-capability shape rather than reinventing one**, adjusted only for:
  (a) our render loop is component-tree-wide re-render for v1, not Terminus's
  eventual fine-grained subtree re-render (their own code comments call the current
  wiring "a stub" for "the initial full-frame re-render implementation" — so v1 Terminus
  itself doesn't yet exploit fine-grained tracking either, which caps how much
  complexity we need to match on day one); (b) our `Listener`/subscriber bookkeeping
  needs to be render-thread-safe per TamboUI's threading model (`RESEARCH.md`, TamboUI
  section) since `Var.set` can conceivably be called from event-handling code.

## cue4s (Scala 3, neandertech) — narrower, but a good cross-platform + effect-API reference

- **Scope**: CLI *prompts* only (single/multi-choice, text, int/float with validation),
  not a general widget/layout framework. Not a widget-ecosystem reference, but valuable
  for two orthogonal things:
  1. **Cross-platform build**: targets JVM, JS, and **Scala Native** from one codebase
     (`%%%` sbt cross-building), reinforcing Terminus's precedent that Scala Native is a
     live option for a native binary, independent of GraalVM.
  2. **Effect-agnostic API surface**: exposes both a synchronous, direct-style API
     (`Prompts.sync.use`, `.getOrThrow`) *and* a `Future`-based API for JS, plus an
     optional Cats Effect integration module. The core API itself returns
     `Completion[CompletionError, A]` / `Either`, decoupled from any specific effect
     type, with thin adapters layered on top per platform/effect-system. This is a good
     model for how our widgets/event API should avoid hard-coupling to a single effect
     type (e.g. don't bake in `cats.effect.IO` at the core layer) while still offering an
     optional `cats-effect` integration module, mirroring cue4s's `modules/cats-effect`.
  3. **Validated builder API**: `.mapValidated(...)`, fluent min/max/positive validators
     on prompt builders — a small but pleasant example of a fluent Scala 3 builder DSL.
     The actual shape, from `modules/core/src/main/scala/Prompt.scala`:
     ```scala
     trait Prompt[Result]:
       self =>
       private[cue4s] def framework(terminal: Terminal, output: Output,
                                     theme: Theme, symbols: Symbols): PromptFramework[Result]

       def map[Derived](f: Result => Derived): Prompt[Derived] =
         mapValidated(r => Right(f(r)))

       def mapValidated[Derived](f: Result => Either[PromptError, Derived]): Prompt[Derived] =
         new Prompt[Derived]:
           override def framework(terminal: Terminal, output: Output,
                                   theme: Theme, symbols: Symbols) =
             self.framework(terminal, output, theme, symbols).mapValidated(f)
     ```
     `Prompt[Result]` is a genuine covariant-in-spirit functor: every specific prompt
     (`Prompt.Input`, `Prompt.MultipleChoice`, …) is a case class carrying only its own
     config, and `map`/`mapValidated` build a *new* `Prompt[Derived]` that wraps the
     original's `framework(...)` call and post-processes its result — the transform is
     stored, not applied eagerly, and only runs once the prompt actually executes. This
     is the same "small typed value that describes what to build, plus a stack of
     lazy/composable transforms over its eventual result" shape as Terminus's `Reactive`
     `map`. Worth carrying into `tui-dsl`'s `Form`/validated-input design (`PLAN.md`
     §5, Tier 2 `Form`): model per-field validation as `Field[A].mapValidated(...)`
     composing the same way, rather than an imperative validate-then-branch style.

## Textual (Python, Textualize) — the richest application framework of the four

Textual is the most "batteries-included application framework" of the reference set —
closer to a web framework (CSS, DOM, async message bus) transplanted into the terminal
than to a rendering library. It also runs the *same app* in a real terminal or a browser
(via a web driver), which is out of scope for us but shapes how cleanly they separate
"driver" from everything above it — a separation worth copying even without a web target.

### Architecture, by source file (`src/textual/`)

- **`app.py` + `compose.py`**: the app entry point. Apps subclass `App` and implement
  `compose(self) -> ComposeResult`, a generator that `yield`s child widgets — Python's
  `yield`-based composition is the direct analog of what a Scala 3 DSL would express
  with a builder/varargs `apply`, e.g. `panel("Title")(child1, child2)` from our
  `PLAN.md` §4. `App.CSS` is a class-level string of Textual CSS scoped to that app.
- **`widget.py` / `widgets/`**: ~30 built-in widgets — `Button`, `Checkbox`, `RadioSet`/
  `RadioButton`, `Switch`, `Input`, `MaskedInput`, `TextArea` (with tree-sitter-backed
  syntax highlighting), `DataTable`, `Tree`, `DirectoryTree`, `ListView`/`ListItem`,
  `OptionList`, `Select`, `SelectionList`, `TabbedContent`/`Tabs`/`TabPane`,
  `Collapsible`, `Markdown`/`MarkdownViewer`, `ProgressBar`, `Sparkline`, `Digits`,
  `Rule`, `RichLog`, `Log`, `LoadingIndicator`, `Toast`, `Tooltip`, `Placeholder`,
  `Label`, `Link`, `Header`/`Footer`, `Welcome`, `HelpPanel`/`KeyPanel`. This is
  strictly the largest widget catalog of the four — in particular `DataTable`, `Tree`,
  `TextArea` (syntax-highlighted code editing), and `DirectoryTree` are more
  sophisticated than anything in ratatui/TamboUI's default sets and worth scoping as
  stretch goals once our Tier 0–3 backlog (`PLAN.md` §5) is solid.
- **`css/`**: a genuinely complete CSS engine — `tokenizer.py`/`parse.py`/`model.py`
  (lexer + parser + AST), `stylesheet.py` (cascade/specificity resolution),
  `scalar.py`/`scalar_animation.py`/`transition.py` (animatable values, CSS
  transitions), `query.py` (selector matching against the widget tree),
  `_styles_builder.py`. This is a much deeper investment than TamboUI's TCSS
  (RESEARCH.md, TamboUI section) — Textual treats styling as a first-class subsystem,
  not a convenience layer. For v1 scope this is *not* worth matching in full (see
  `PLAN.md` — CSS-level theming is explicitly out of v1 scope), but the layering
  (tokenizer → parser → cascade → per-widget computed style object) is the right shape
  to grow into later if we ever add a themeable stylesheet layer beyond the fluent
  Scala 3 styling extension methods planned for `tui-dsl`.
- **`dom.py`**: widgets form a DOM-like tree (`DOMNode`) that CSS selectors query
  against (`query_one(SelectorOrType)`, seen in the README example:
  `self.query_one(Digits).update(...)`). This is the piece that makes Textual's CSS
  engine actually useful — selectors need a real tree to match against, not just a flat
  widget list. If we ever grow a query API in `tui-dsl`, model it on this pairing
  (tree + selector) rather than adding query methods ad hoc per container.
- **`reactive.py`**: the `Reactive[T]` descriptor — a class-level reactive attribute
  (`count = reactive(0)`) that triggers `watch_<name>` callbacks and a re-render when
  set, with optional `compute_<name>` derived attributes and `always_update`/`init`
  flags. This is Textual's answer to "how does state change trigger re-render," and
  it's a good, concrete reference point for `PLAN.md` §4's "retained-mode component
  tree... closer to Terminus's `React`/`Reactive` model" — Textual's `Reactive`
  descriptor is effectively what that idea looks like fully fleshed out in a dynamic
  language. In Scala 3 the equivalent would lean on `given`/opaque state holders or a
  small signal/observable primitive in `tui-runtime`, watched by macro-generated
  `watch`/`compute` wiring in `tui-macros` rather than Python's dunder-method magic.
- **`message.py` / `message_pump.py` / `_context.py`**: an internal async message-bus
  architecture — every `DOMNode` is a `MessagePump` with its own asyncio queue; events
  (`events.py`) and user messages are dispatched through it, bubbling up the DOM tree
  unless a handler stops propagation. This is Textual's Elm/Redux-ish backbone, async
  from the ground up (the README explicitly calls out "Textual is an asynchronous
  framework under the hood"). Our `tui-runtime` (`PLAN.md` §3) plays the same role but
  is planned synchronous-first per RESEARCH.md's cue4s takeaway (effect-agnostic core,
  no hard-coded async runtime) — worth an explicit note in the Plan that "bubbling
  event propagation up a widget tree" is a feature worth keeping even without adopting
  Textual's asyncio coupling.
- **`driver.py` / `drivers/`**: terminal I/O abstracted behind a `Driver` interface,
  with concrete `linux_driver.py`, `windows_driver.py`, `web_driver.py`, and a
  `headless_driver.py` used purely for testing (see `pilot.py` below). The
  headless/testable driver is the standout idea here: it means the entire app can run
  full event/render cycles with no real TTY attached, driven programmatically.
- **`pilot.py`**: `App.run_test()` returns a `Pilot` that can synthesize key presses,
  clicks, and mouse events against a running app instance (backed by
  `HeadlessDriver`), and wait for the app to reach an idle/settled state before
  asserting. This is the most directly actionable idea here, and it **was adopted
  into the plan**: `PLAN.md` §9 specifies a headless `Backend` implementation
  (same `Backend` trait as the real JLine backend but writing to an in-memory buffer
  and accepting synthetic input) plus a `Pilot`-equivalent test helper in the
  `test-support/` module, letting `tui-examples` apps be tested end-to-end
  (press-key → assert-rendered-output) without spawning a real terminal.
- **`worker.py` / `worker_manager.py`**: managed background-task API (`@work` decorator)
  so widgets can kick off async work without manually managing asyncio tasks/cancellation.
  Relevant only if/when we add an optional async integration module — not v1 scope, but
  the pattern (framework-managed cancellable background work tied to widget lifecycle)
  is worth remembering if `tui-cats-effect` (mentioned as a possible future module in
  `PLAN.md` §4) ever gets built.
- **`screen.py`**: a screen *stack* (push/pop full-screen or modal `Screen`s), which is
  how Textual implements dialogs/modals rather than a single always-visible root widget.
  Ratatui/TamboUI instead render everything through one `Frame`/root widget per draw and
  build dialogs as widgets drawn on top (TamboUI has a `DialogElement`, RESEARCH.md
  TamboUI section, Tier 4 in `PLAN.md` §5). Textual's screen-stack is more powerful (real
  navigation, each screen keeps its own state/CSS) but heavier; not needed for v1 — the
  TamboUI-style "dialog is just a widget drawn last" approach is enough for the widget
  backlog as scoped — but worth a one-line flag as a stretch architectural option if
  multi-screen navigation ever becomes a real requirement.

### What's distinctive vs. the other three

Textual is the only one of the four that treats **CSS-based styling, a DOM+selector
query model, and async message-passing** as core architecture rather than optional
add-ons — TamboUI's `tamboui-css` module is explicitly modeled on the same idea (TCSS)
but is one module among many, not the organizing principle. For this plan, Textual is
best read as "how far this could go" rather than a v1 blueprint: its widget catalog
breadth (`DataTable`, `Tree`, `TextArea`) and its headless-driver/`Pilot` testing model
are worth adopting; its full CSS-cascade engine and asyncio-first message bus are
deliberately **not** adopted for v1 (see `PLAN.md` — kept as documented future
directions instead, consistent with keeping `tui-runtime`'s core effect-agnostic per
the cue4s takeaway).

## Synthesis: what to take from each

| From | Take |
|---|---|
| ratatui | crate-splitting-by-stability discipline (core/widgets/backend/facade); `Constraint`/`Layout` vocabulary; confirms the baseline widget catalog (near-identical to TamboUI's) |
| TamboUI | three-tier API (immediate / mid-level runner / declarative toolkit); widget catalog; render-thread model; reflection-avoidance discipline for native-image; `CharWidth` utility; module boundaries as a template for our Mill modules |
| Terminus | idiomatic Scala 3 terminal-control primitives (`core`); its `Var`/`Computed`/`React`-capability fine-grained signals system (`ui`), taken near-verbatim as the base for `tui-runtime`'s reactive state primitive, not just "inspiration" |
| cue4s | effect-agnostic core API + optional cats-effect module; cross-platform (JVM/Native) build precedent in pure Scala 3; `Prompt[Result]`'s lazy `map`/`mapValidated` functor shape as the model for `Form` field validation |
| Textual | headless driver + `Pilot`-style programmatic testing harness (adopted for v1 — see `PLAN.md` §9); DOM+selector query model and reactive-attribute descriptor as reference points if `tui-dsl` grows a query API; widest widget catalog (`DataTable`, `Tree`, `TextArea`) as a stretch-goal reference; full CSS cascade engine and asyncio-first message bus noted as deliberately out of v1 scope |
