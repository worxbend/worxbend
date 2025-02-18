import os
import subprocess
import shutil
import re
import sys
from pathlib import Path

def get_cwd():
    return os.getcwd()


def is_git_installed():
    return shutil.which("git") is not None


def get_git_root():
    return subprocess.check_output(["git", "rev-parse", "--show-toplevel"]).strip().decode("utf-8")


if not is_git_installed():
    print("ERROR: Git is not installed.")
    exit(1)

project_root = get_git_root()

root_build_sbt = os.path.join(project_root, "build.sbt")

project_slug = '{{ cookiecutter.project_slug }}'

BUILD_SBT_PATH = root_build_sbt


def read_build_sbt():
    """Reads the build.sbt file."""
    return Path(BUILD_SBT_PATH).read_text(encoding="utf-8")


def write_build_sbt(content):
    """Writes the updated content back to build.sbt."""
    Path(BUILD_SBT_PATH).write_text(content, encoding="utf-8")

    return content


def update_build_sbt(content, project_type, project_name, project_path):
    """Updates build.sbt by inserting the project in the correct region and sorting entries."""


    region_pattern = {
        "app": re.compile(r"(//\s*region apps\n)([\s\S]+?)(//\s*endregion apps)"),
        "lib": re.compile(r"(//\s*region libs\n)([\s\S]+?)(//\s*endregion libs)"),
    }

    project_definition = f"""
lazy val `{project_name}` = ProjectRef(
  base = file("{project_path}"),
  id = "{project_name}",
)
""".strip()

    if project_type not in region_pattern:
        print("Invalid project type. Use 'app' or 'lib'.")
        sys.exit(1)

    # Find the correct region
    match = region_pattern[project_type].search(content)
    if not match:
        print(f"Could not find the {project_type} section in build.sbt.")
        sys.exit(1)

    start, region_content, end = match.groups()

    # Extract existing entries
    entries = re.findall(r"(\/\* >>-------------------------------<< \*\/\n?lazy val `*\w+`* = ProjectRef\([\s\S]+?\)[\s\S]+?\/* >>-------------------------------<< \*\/)", region_content)

    def wrap_entry(entry):
        return "/* >>-------------------------------<< */\n" + entry + "\n/* >>-------------------------------<< */"

    # Add new entry and sort alphabetically
    updated_entries = sorted(set(entries + list(map(wrap_entry, [project_definition]))))

    # Replace region with sorted entries
    updated_region = "\n\n\n".join(updated_entries)

    updated_region = "\n" + updated_region + "\n\n"
    updated_content = content.replace(region_content, updated_region)

    return updated_content


def update(project_type, project_name, project_path):
    content = read_build_sbt()
    updated_content = update_build_sbt(content, project_type, project_name, project_path)
    updated_content = add_to_root_project(updated_content, project_name, project_type)
    updated_content = write_build_sbt(updated_content)
    print(f"Updated build.sbt with {project_name} successfully")
    return updated_content


def add_to_root_project(content, project_name, project_type):
    """Adds the application to the root project aggregate list."""
    aggregate_region_pattern = {
        "app": re.compile(r"(// region aggregate-apps)([\s\S]+?)(//\s*endregion aggregate-apps)"),
        "lib": re.compile(r"(// region aggregate-libs)([\s\S]+?)(//\s*endregion aggregate-libs)"),
    }
    # Find the correct region
    match = aggregate_region_pattern[project_type].search(content)
    if not match:
        print("Could not find the root project aggregate list.")
        return content

    start, region_content, end = match.groups()

    # Add project and maintain alphabetical order
    existing_apps = re.findall(r"(`*\w+`*,)", region_content)
    updated_apps = sorted(set(existing_apps + [f"`{project_name}`,"]))
    updated_apps = list(map(lambda x: "    " + x, updated_apps))
    updated_region = "\n".join(updated_apps)

    updated_region = "\n" + updated_region + "\n    "
    return content.replace(region_content, updated_region)

application_path = get_cwd()
application_path = application_path.replace(project_root, "").lstrip("/")

print(application_path)
if application_path.startswith("applications"):
    update("app", project_slug, application_path)
elif application_path.startswith("libs"):
    update("lib", project_slug, application_path)
else:
    print("ERROR: Not valid template.")
    exit(1)
