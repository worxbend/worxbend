FROM python:3.11-slim

ENV DEBIAN_FRONTEND=noninteractive

RUN apt update && \
    apt -y install --no-install-recommends curl

ENV POETRY_HOME=/opt/poetry

RUN curl -sSL https://install.python-poetry.org | python3 -

ENV PATH="${PATH}:$POETRY_HOME/bin"

RUN apt purge curl && \
    apt autoclean && \
    apt autoremove

