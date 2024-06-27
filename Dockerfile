FROM eclipse-temurin:17-jre@sha256:171e90d2ca55e6958d8b56b58670fe42e9986c540225ce9f61a67b477017c217

RUN apt-get update -yqq && apt-get upgrade -yqq && \
    apt-get autoremove -y && apt-get clean && rm -rf /var/lib/apt/lists/

WORKDIR /opt/codex-feasibility-backend
COPY ./target/*.jar ./feasibility-gui-backend.jar
COPY ontology ontology

ARG VERSION=2.1.0
ENV APP_VERSION=${VERSION}
ENV FEASIBILITY_DATABASE_HOST="feasibility-network"
ENV FEASIBILITY_DATABASE_PORT=5432
ENV FEASIBILITY_DATABASE_USER=postgres
ENV FEASIBILITY_DATABASE_PASSWORD=password
ENV CERTIFICATE_PATH=/opt/codex-feasibility-backend/certs
ENV TRUSTSTORE_PATH=/opt/codex-feasibility-backend/truststore
ENV TRUSTSTORE_FILE=self-signed-truststore.jks

RUN mkdir logging && \
    mkdir -p $CERTIFICATE_PATH $TRUSTSTORE_PATH && \
    chown -R 10001:10001 /opt/codex-feasibility-backend && \
    chown 10001:10001 $CERTIFICATE_PATH $TRUSTSTORE_PATH
USER 10001

HEALTHCHECK --interval=5s --start-period=10s CMD curl -s -f http://localhost:8090/actuator/health || exit 1

COPY ./docker-entrypoint.sh /
ENTRYPOINT ["/bin/bash", "/docker-entrypoint.sh"]