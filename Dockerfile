FROM eclipse-temurin:17-jre

RUN apt update -yqq && apt upgrade -yqq && \
    apt-get autoremove -y && apt-get clean && rm -rf /var/lib/apt/lists/

WORKDIR /opt/dataportal-backend
COPY ./target/*.jar ./dataportal-backend.jar
COPY ontology ontology

RUN groupadd --system dataportal && useradd --system dataportal -g dataportal
RUN mkdir logging
RUN chown -R dataportal:dataportal /opt/dataportal-backend

USER dataportal:dataportal

ARG VERSION=6.0.0
ENV APP_VERSION=${VERSION}
ENV DATABASE_HOST="dataportal-network"
ENV DATABASE_PORT=5432
ENV DATABASE_USER=postgres
ENV DATABASE_PASSWORD=password
ENV CERTIFICATE_PATH=/opt/dataportal-backend/certs
ENV TRUSTSTORE_PATH=/opt/dataportal-backend/truststore
ENV TRUSTSTORE_FILE=self-signed-truststore.jks

RUN mkdir -p $CERTIFICATE_PATH $TRUSTSTORE_PATH
RUN chown dataportal:dataportal $CERTIFICATE_PATH $TRUSTSTORE_PATH

HEALTHCHECK --interval=5s --start-period=10s CMD curl -s -f http://localhost:8090/actuator/health || exit 1

COPY ./docker-entrypoint.sh /
ENTRYPOINT ["/bin/bash", "/docker-entrypoint.sh"]

ARG GIT_REF=""
ARG BUILD_TIME=""
LABEL maintainer="medizininformatik-initiative" \
    org.opencontainers.image.created=${BUILD_TIME} \
    org.opencontainers.image.authors="medizininformatik-initiative" \
    org.opencontainers.image.source="https://github.com/medizininformatik-initiative/feasibility-backend" \
    org.opencontainers.image.version=${VERSION} \
    org.opencontainers.image.revision=${GIT_REF} \
    org.opencontainers.image.vendor="medizininformatik-initiative" \
    org.opencontainers.image.title="dataportal backend" \
    org.opencontainers.image.description="Provides backend functions for the dataportal"
