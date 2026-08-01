# TUI Library — Planning

Planning space for building a Scala 3 library for terminal user interfaces (TUI),
targeting the JVM (with a GraalVM native-image target) and Mill as the build tool.

**This directory holds planning docs only.** The library itself is implemented in a
separate, standalone repository (not nested under `worxbend`'s `libs/`) — see
`PLAN.md` §3 for the repo/module layout this plan targets.

Documents:

- [`RESEARCH.md`](RESEARCH.md) — findings from analyzing ratatui, TamboUI, Terminus,
  cue4s, and Textual, including concrete type signatures and code excerpts pulled
  directly from each reference implementation.
- [`SPEC.md`](SPEC.md) — the technical specification: concrete Scala 3 type/trait
  signatures for every module, package/versioning conventions, an explicit non-goals
  table, and an open-decisions log. Authoritative over `PLAN.md` where the two
  disagree.
- [`PLAN.md`](PLAN.md) — the phased development plan, written to be handed to an AI
  coding agent for autonomous implementation: module architecture, DSL design goals,
  widget backlog, native-image strategy, examples, testing strategy, execution order,
  acceptance criteria, risk register, and CI/docs plan.

Read in order: `RESEARCH.md` → `SPEC.md` → `PLAN.md`.

## Reference projects

| Project | Language | Repo |
|---|---|---|
| ratatui | Rust | https://github.com/ratatui/ratatui |
| TamboUI | Java | https://github.com/tamboui/tamboui |
| Terminus | Scala 3 | https://github.com/creativescala/terminus |
| cue4s | Scala 3 | https://github.com/neandertech/cue4s |
| Textual | Python | https://github.com/Textualize/textual |

These five are **read-only reference material** — architecture and API shape to learn
from, never a source to copy code from. See `PLAN.md` §2.1 for the explicit
originality mandate.

## Design guidelines

| Project | Purpose |
|---|---|
| [w0rxbend/ai-oop-design-patterns](https://github.com/w0rxbend/ai-oop-design-patterns) | OOP fundamentals, SOLID, and GoF-pattern guidelines (plus Claude Code skills) to apply throughout implementation — see `PLAN.md` §2.2 |

Reference clones for local analysis live outside the repo (scratch/tmp, not committed) —
re-clone with `--depth 1` as needed; see "Research method" in `RESEARCH.md`.
