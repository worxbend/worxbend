import os
import subprocess
import shutil
import sys
import re

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


def add_application_to_build_sbt(build_sbt_path, app_name):
    with open(build_sbt_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Define the new project reference block
    new_project_ref = f"""
/* >>-------------------------------<< */
lazy val `{app_name}` = ProjectRef(
  base = file("applications/{app_name}"),
  id = "{app_name}",
)
/* >>-------------------------------<< */
"""

    # Insert the new project in the Applications section
    applications_section_pattern = r"(/\* >>----- BEGIN: Applications -----<< \*/)"
    content = re.sub(applications_section_pattern, r"\1\n" + new_project_ref, content, count=1)

    # Find the aggregate list and add the new application
    aggregate_section_pattern = r"(\.aggregate\(\n(?:\s*/\* >>----------<< \*/\n)*.*?)(/\* >>----------<< \*/\n\s*/\* > libraries < \*/)"
    match = re.search(aggregate_section_pattern, content, re.DOTALL)

    if match:
        aggregate_section = match.group(1)
        libraries_marker = match.group(2)
        new_aggregate_entry = f"    `{app_name}`,\n"
        updated_aggregate_section = aggregate_section + new_aggregate_entry
        content = content.replace(aggregate_section + libraries_marker, updated_aggregate_section + libraries_marker)

    with open(build_sbt_path, "w", encoding="utf-8") as f:
        f.write(content)

    print(f"Added new application `{app_name}` to build.sbt")



add_application_to_build_sbt(root_build_sbt, project_slug)
