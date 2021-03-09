# CODEX Feasibility Backend

## Running the DSF Path

In order to run the backend using the DSF path, the following environment variables need to be set:

| EnvVar | Description | Example |
|--------|-------------|---------|
| DSF_SECURITY_CACERT | Certificate required for secured communication with the DSF middleware. ||
| DSF_SECURITY_KEYSTORE_P12FILE | Security archive (`PKCS #12`) carrying authentication information required for communication with the DSF middleware. ||
| DSF_SECURITY_KEYSTORE_PASSWORD | Password required to decrypt the security archive for subsequent use. ||
| DSF_WEBSERVICE_BASE_URL | Base URL pointing to the local ZARS FHIR server. | `https://zars/fhir` |
| DSF_WEBSOCKET_URL | URL pointing to the local ZARS FHIR server websocket endpoint. | `wss://zars/fhir/ws` |
| DSF_ORGANIZATION_ID | Identifier for the local organization this backend is part of. | `MY ZARS` |


# Database

## Running database

```
docker run --name codex_ui -d -e POSTGRES_PASSWORD=password -e POSTGRES_DB=codex_ui -p 5555:5432 postgres:alpine
```
