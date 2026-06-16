import os
import subprocess
import shutil
from pathlib import Path


def is_git_installed() -> bool:
    return shutil.which("git") is not None


def get_git_root() -> str:
    return subprocess.check_output(
        ["git", "rev-parse", "--show-toplevel"],
        text=True,
    ).strip()


if not is_git_installed():
    print("ERROR: Git is not installed.")
    exit(1)

project_root = get_git_root()
project_slug = "{{ cookiecutter.project_slug }}"
generated_path = os.getcwd()
relative_path = generated_path.replace(project_root, "").lstrip("/")

if not (relative_path.startswith("applications") or relative_path.startswith("libs")):
    print(f"ERROR: Generated path '{relative_path}' is not under applications/ or libs/.")
    exit(1)

package_mill = Path(generated_path) / "package.mill"
if package_mill.exists():
    print(f"Mill module '{project_slug}' created at {relative_path}")
    print("Mill discovers the module automatically via package.mill on the next build.")
else:
    print(f"ERROR: package.mill was not created at {generated_path}")
    exit(1)
