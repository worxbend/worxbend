import os
import subprocess
import shutil


def is_git_installed():
    return shutil.which("git") is not None


def get_git_root():
    return subprocess.check_output(["git", "rev-parse", "--show-toplevel"]).strip().decode("utf-8")


if not is_git_installed():
    print("ERROR: Git is not installed.")
    exit(1)

project_root = get_git_root()

root_build_sbt = os.path.join(project_root, "build.sbt")

with open(root_build_sbt, "a") as f:
  pass
