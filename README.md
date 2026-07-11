<div align="center">

# ✨ worxbend ✨

### a personal monorepo where scala experiments go to live (or die trying) 💀🧪

[![StandWithUkraine](https://raw.githubusercontent.com/vshymanskyy/StandWithUkraine/main/badges/StandWithUkraine.svg)](https://github.com/vshymanskyy/StandWithUkraine/blob/main/docs/README.md)
[![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/worxbend/worxbend?labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit%20Reviews)](https://coderabbit.ai)
![License](https://img.shields.io/badge/license-GPL--2.0-blue)
![Build](https://img.shields.io/badge/build-Mill-brightgreen)
![Vibe](https://img.shields.io/badge/vibe-chaotic%20good-purple)

![banner](https://github.com/worxbend/worxbend/assets/8176996/7c4d7c52-e45b-4558-b3c1-ac79dbd1a2d9)

</div>

---

## 🧭 What even is this?

Hey 👋 welcome to my digital junk drawer.

This is a **personal monorepo and learning playground** 🎓 — a pile of small Scala/JVM apps, libraries,
infra recipes, and notes-to-future-me, all built with [Mill](https://mill-build.org/) 🏗️.

Nothing in here is trying to be a "product". It's mostly:

- 🧪 experiments and things I wanted to learn by building
- 🛠️ small tools I actually use day-to-day
- 📚 notes so I remember how I solved something six months ago
- 🏠 self-hosted infra recipes for my homelab

If it looks unpolished in places — it's because it's a workshop, not a showroom. 🔧

## 🗂️ Repo layout

```
worxbend/
├── applications/     🚀 small Scala apps & services — experiments and personal tools
├── libs/             📦 reusable Scala libraries shared across the applications
├── docs/             📝 notes, guides, research, and other braindumps
├── deployments/      🐳 docker-compose stacks for self-hosted infrastructure
├── .ansible/         🤖 ansible playbooks for infra automation
├── .cookiecutter/    🍪 templates for scaffolding new Scala modules
└── justfile          ⚡ task runner recipes (scaffolding, cleanup, etc.)
```

## 🕵️ A note on naming

You'll notice the projects under `applications/` and `libs/` have... weird names. `Astrion`, `Calyx`, `Nebula`,
`Umbra`, `Kvetch`, `Sybella` 🌌 — a grab-bag of cool-sounding, mythical/cosmic/random words.

That's on purpose. 😏 Some of these projects aren't meant to be easily searchable or self-explanatory from the
outside — the codename tells you nothing about what's inside. If you want to know what a project actually does,
you have to open it up. Consider it mild, harmless obfuscation-by-vibes rather than a real security boundary. 🔮

## 🚀 Getting started

### Clone it

```bash
git clone git@github.com:worxbend/worxbend.git
cd worxbend
```

### Prerequisites

You'll want:

- ☕ a JDK — see `.sdkmanrc` for the exact version ([SDKMAN!](https://sdkman.io/) will pick it up automatically)
- 🏗️ [Mill](https://mill-build.org/) — the repo ships its own `./mill` bootstrap script, so you don't need it
  installed globally
- ⚡ [`just`](https://just.systems/) — only needed if you want to scaffold new modules (see below)
- 🐍 Python 3 + [`pipx`](https://pipx.pypa.io/) — only needed for the cookiecutter scaffolding, via a venv

### Import into your IDE

Point IntelliJ / Metals / your editor of choice at the repo root — Mill's BSP support will pick up all the
modules under `applications/` and `libs/` automatically. Metals scratch files live under `.metals/`, already
gitignored. 🧠

### Build & run

```bash
./mill resolve _                 # list all modules
./mill <module>.run               # run a specific application
./mill __.test                    # run all tests
```

## ⚡ `just` — the task runner

This project uses [`just`](https://just.systems/) to automate a handful of routines, mainly scaffolding new
modules. Install it via your package manager:

```bash
# Debian/Ubuntu
sudo apt install just

# Fedora
sudo dnf install just

# Arch Linux
sudo pacman -S just
```

Or grab a prebuilt binary from the [releases page](https://github.com/casey/just/releases).

Verify it's there:

```bash
just --version
```

> [!NOTE]
> The main thing `just` does here is drive cookiecutter to scaffold new Mill modules. If you're fine writing the
> boilerplate by hand, you can skip installing it entirely.

## 🍪 Bootstrapping a new project (cookiecutter)

New apps and libs are scaffolded from a [cookiecutter](https://cookiecutter.readthedocs.io/) template in
`.cookiecutter/scala/app`, driven through `just`:

```bash
just create-main-scala-app              # new app straight under applications/
just create-scala-app <group-prefix>    # new app under applications/<group-prefix>/
just create-main-scala-lib              # new lib straight under libs/
just create-scala-lib <group-prefix>    # new lib under libs/<group-prefix>/
```

You'll be prompted for a project name, package prefix, license, and Scala version — the template wires up
`package.mill`, `.scalafmt.conf`, and the base source layout for you. ✨ Pick a suitably weird codename while
you're at it (see [naming](#-a-note-on-naming) above 🌌).

### Python venv for cookiecutter

The scaffolding is invoked via `pipx run cookiecutter`, so you'll need Python 3 and `pipx` available. A venv
isn't strictly required since `pipx` manages its own isolated environment, but if you prefer one:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install pipx
```

## 📚 Docs

Random notes, guides, and research live under [`docs/`](docs/) — everything from infra write-ups to reading
lists to "why did I do it this way" notes. Also worth a peek:

- [`CODE_STYLE.md`](CODE_STYLE.md) 📐
- [`SCALA_CODE_STYLE.md`](SCALA_CODE_STYLE.md) 🎯

## 🏗️ Infra

Self-hosted stacks for my homelab live under [`deployments/docker-compose-files`](deployments/docker-compose-files)
(Kafka, VictoriaMetrics, Couchbase, UniFi controller, and friends 🐳), with [`.ansible/`](.ansible) covering the
provisioning side.

## 📄 License

GPL-2.0 — see [`LICENSE`](LICENSE).
