FROM eclipse-temurin:17-jre

RUN apt update -yqq && apt upgrade -yqq && \
    apt-get autoremove -y && apt-get clean && rm -rf /var/lib/apt/lists/

WORKDIR /opt/mii-feasibility-backend
COPY ./target/*.jar ./feasibility-gui-backend.jar
COPY ontology ontology

RUN groupadd --system feasibility && useradd --system feasibility -g feasibility
RUN mkdir logging
RUN chown -R feasibility:feasibility /opt/mii-feasibility-backend

USER feasibility:feasibility

ARG VERSION=5.0.1
ENV APP_VERSION=${VERSION}
ENV FEASIBILITY_DATABASE_HOST="feasibility-network"
ENV FEASIBILITY_DATABASE_PORT=5432
ENV FEASIBILITY_DATABASE_USER=postgres
ENV FEASIBILITY_DATABASE_PASSWORD=password
ENV CERTIFICATE_PATH=/opt/mii-feasibility-backend/certs
ENV TRUSTSTORE_PATH=/opt/mii-feasibility-backend/truststore
ENV TRUSTSTORE_FILE=self-signed-truststore.jks

RUN mkdir -p $CERTIFICATE_PATH $TRUSTSTORE_PATH
RUN chown feasibility:feasibility $CERTIFICATE_PATH $TRUSTSTORE_PATH

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
    org.opencontainers.image.title="feasibility backend" \
    org.opencontainers.image.description="Provides backend functions for feasibility UI including query execution"
