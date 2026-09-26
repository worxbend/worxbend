FROM python:3.11-slim

# keeps Python from buffering our standard output stream,
# which means that logs can be delivered to the user quickly.
ENV PYTHONUNBUFFERED=1

# The temporary directory used by pip during the installation process is not needed after the installation is complete.
ENV PIP_NO_CACHE_DIR=off \
    PIP_DISABLE_PIP_VERSION_CHECK=on \
    PIP_DEFAULT_TIMEOUT=100 \
    POETRY_VIRTUALENVS_CREATE=false \
    POETRY_NO_INTERACTION=1

# Install Poetry from PyPI with pip — pip downloads over HTTPS and verifies the
# artifacts it installs — instead of piping a downloaded installer script
# straight into the interpreter.
ARG POETRY_VERSION=2.5.1
RUN pip install --no-cache-dir "poetry==${POETRY_VERSION}" && \
    useradd --create-home --uid 1000 poetry

# Run as an unprivileged user by default.
USER poetry

# Validate installation
RUN poetry --version
