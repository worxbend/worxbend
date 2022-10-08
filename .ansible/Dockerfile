FROM python:alpine3.16

ARG username=alpine
ARG home_dir=/home/${username}

RUN addgroup -g 1000 groupname
RUN adduser -u 1000 -G groupname -h ${home_dir} -D ${username}

RUN apk update && apk add \
    curl \
    openssh-client \
    sshpass \
    vim
RUN pip install --no-cache-dir ansible ansible-core

USER ${username}
WORKDIR ${home_dir}

CMD ./start.sh
