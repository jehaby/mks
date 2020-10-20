FROM node:14.14

WORKDIR /tmp
ENV CLOJURE_VER=1.10.1.716

RUN apt-get update \
  && apt-get -q -y install openjdk-8-jdk curl \
  && curl -s https://download.clojure.org/install/linux-install-$CLOJURE_VER.sh | bash

ARG UID
ARG GUID

USER root
RUN usermod -u $UID node
RUN groupmod -g $GUID node

WORKDIR /app

USER node:node

CMD ["sh"]
