# Worxbend

[![StandWithUkraine](https://raw.githubusercontent.com/vshymanskyy/StandWithUkraine/main/badges/StandWithUkraine.svg)](https://github.com/vshymanskyy/StandWithUkraine/blob/main/docs/README.md)
![238161209-a39b1b5f-0231-4ffc-a89d-8c4821baca63](https://github.com/worxbend/worxbend/assets/8176996/7c4d7c52-e45b-4558-b3c1-ac79dbd1a2d9)

## Intro

...

## Prerequisites:

### Installing `just`

This project utilizes `just` to automate various routines. Ensure that you have it installed on your system before
proceeding:

1. Install `just` using your distribution's package manager:
    - **Debian/Ubuntu**:
      ```bash
      sudo apt install just
      ```
    - **Fedora**:
      ```bash
      sudo dnf install just
      ```
    - **Arch Linux**:
      ```bash
      sudo pacman -S just
      ```

2. Alternatively, you can download the latest release from the [official
   `just` GitHub releases page](https://github.com/casey/just/releases) and add it to your system.

#### Verifying the Installation:

After installation, verify that `just` is installed correctly by running:

```bash
just --version
```

#### Additional Notes:

- For more information or troubleshooting, refer to the official [`just` documentation](https://just.systems/).
- If working in a containerized environment, you can install `just` within the container by using the appropriate
  package manager or by downloading the binary.

With `just` installed, you're ready to proceed with the project.

> ![NOTE]
> The sole functionality provided by `just` is creating new sbt modules. If you prefer to handle the boilerplate tasks
> manually when adding a new project (whether it's an application or a library), you can skip this step.


### Setting python venv up:

---

