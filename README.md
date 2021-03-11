# CODEX Feasibility Backend

## Configuration

| EnvVar | Description | Example |
|--------|-------------|---------|
|BROKER_CLIENT_TYPE|Selects the BorkerClient implementation to be used. Valid types are: `DSF`, `AKTIN`, `MOCK`| `DSF` |

### Running the DSF Path

In order to run the backend using the DSF path, the following environment variables need to be set:

| EnvVar | Description | Example |
|--------|-------------|---------|
| DSF_SECURITY_CACERT | Certificate required for secured communication with the DSF middleware. ||
| DSF_SECURITY_KEYSTORE_P12FILE | Security archive (`PKCS #12`) carrying authentication information required for communication with the DSF middleware. ||
| DSF_SECURITY_KEYSTORE_PASSWORD | Password required to decrypt the security archive for subsequent use. ||
| DSF_WEBSERVICE_BASE_URL | Base URL pointing to the local ZARS FHIR server. | `https://zars/fhir` |
| DSF_WEBSOCKET_URL | URL pointing to the local ZARS FHIR server websocket endpoint. | `wss://zars/fhir/ws` |
| DSF_ORGANIZATION_ID | Identifier for the local organization this backend is part of. | `MY ZARS` |

## Starting with Docker

### Create Docker image
```
mvn install
docker build -t feasibility-gui-backend . 
```

### Start backend and database
```
docker-compose up -d
```

### Test if container is running properly
```
GET http://localhost:8090/api/v1/terminology/root-entries
```

Should reply with status 200 and a JSON object