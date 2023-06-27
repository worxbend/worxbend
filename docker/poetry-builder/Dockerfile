FROM python:3.11-slim

ENV DEBIAN_FRONTEND=noninteractive
ENV POETRY_HOME=/opt/poetry

RUN apt update && \
    apt -y install --no-install-recommends curl

RUN curl -sSL https://install.python-poetry.org | python3 -

# Add poetry to PATH
ENV PATH="${PATH}:$POETRY_HOME/bin"

RUN apt purge curl -y && \
    apt autoclean && \
    apt autoremove

