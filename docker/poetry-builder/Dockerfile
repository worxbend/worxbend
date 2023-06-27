FROM python:3.11-slim

RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends curl

ENV POETRY_HOME=/opt/poetry
RUN export POETRY_HOME=/opt/poetry
RUN curl -sSL https://install.python-poetry.org | python3 -

ENV PATH="${PATH}:$POETRY_HOME/bin"

