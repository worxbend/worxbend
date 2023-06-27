FROM python:3.11-slim

# keeps Python from buffering our standard output stream,
# which means that logs can be delivered to the user quickly.
ENV PYTHONUNBUFFERED 1

# DEBIAN_FRONTEND=noninteractive prevents the installer from waiting for user input
ENV DEBIAN_FRONTEND=noninteractive

# Poetry installation path
ENV POETRY_HOME=/opt/poetry

# The temporary directory used by pip during the installation process is not needed after the installation is complete.
ENV PIP_NO_CACHE_DIR=off \
    PIP_DISABLE_PIP_VERSION_CHECK=on \
    PIP_DEFAULT_TIMEOUT=100 \
    POETRY_VIRTUALENVS_CREATE=false \
    POETRY_NO_INTERACTION=1 \

RUN apt update && \
    apt -y install --no-install-recommends curl

RUN curl -sSL https://install.python-poetry.org | python3 -

# Add poetry to PATH
ENV PATH="${PATH}:$POETRY_HOME/bin"

# Remove unused packages
RUN apt purge curl -y && \
    apt autoremove -y && \
    apt autoclean -y

# Validate installation
RUN poetry --version

RUN rm -rf /tmp/pip-tmp
