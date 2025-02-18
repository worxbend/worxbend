import re
import sys

MODULE_REGEX = r'^[a-zA-Z][a-zA-Z0-9\-]+$'
project_slug = '{{ cookiecutter.project_slug }}'

if not re.match(MODULE_REGEX, project_slug):
    print(f'ERROR: {project_slug} is not a valid Scala module name!')
    sys.exit(1)
