FROM openjdk:15

WORKDIR /opt/codex-feasibility-backend
COPY ./start.sh .
COPY ./target/*.jar ./feasibility-gui-backend.jar
COPY ontology ontology
RUN mkdir "additional-certs"

ARG VERSION=0.0.0
ENV APP_VERSION=${VERSION}
ENV FEASIBILITY_DATABASE_HOST="feasibility-network"
ENV FEASIBILITY_DATABASE_PORT=5432
ENV FEASIBILITY_DATABASE_USER=postgres
ENV FEASIBILITY_DATABASE_PASSWORD=password

#ENTRYPOINT["start.sh"]
CMD ["./start.sh", "/opt/codex-feasibility-backend/additional-certs/"]

ARG GIT_REF=""
ARG BUILD_TIME=""
LABEL maintainer="num-codex" \
    org.opencontainers.image.created=${BUILD_TIME} \
    org.opencontainers.image.authors="num-codex" \
    org.opencontainers.image.source="https://github.com/num-codex/codex-feasibility-backend" \
    org.opencontainers.image.version=${VERSION} \
    org.opencontainers.image.revision=${GIT_REF} \
    org.opencontainers.image.vendor="num-codex" \
    org.opencontainers.image.title="codex feasibility backend" \
    org.opencontainers.image.description="Provides backend functions for feasibility UI including query execution"
