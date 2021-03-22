# CODEX Feasibility Backend



## Configuration

| EnvVar | Description | Example | Default |
|--------|-------------|---------|---------|
|BROKER_CLIENT_TYPE|Selects the BorkerClient implementation to be used. Valid types are: `DSF`, `AKTIN`, `MOCK`| `DSF` | `MOCK` |
|KEYCLOAK_ENDPOINT| | |`http://localhost:8080`|
|KEYCLOAK_REALM| | |`Num`|
|KEYCLOAK_CLIENT_ID| | |`codexFeasibilityGuiBackend`|
|KEYCLOAK_CLIENT_SECRET| | |`bc1843c0-3c57-4c2c-9b40-ac38e6dd545e`|
|FEASIBILITY_DATABASE_HOST|Host under which the Postgres feasibility database can be reached.|localhost|`localhost`|
|FEASIBILITY_DATABASE_USER|Username to connect to the Postgres feasibility database.|codex-postgres|`codex-postgres`|
|FEASIBILITY_DATABASE_PASSWORD|Password to connect to the Postgres feasibility database.|codex-password|`codex-password`|


### Running the AKTIN broker path

In order to run the backend using the AKTIN broker path, the following environment variables need to be set:

| EnvVar | Description | Example | Default |
|--------|-------------|---------|---------|
| AKTIN_BROKER_BASE_URL | Base URL for the AKTIN RESTful API | http://localhost:8080/broker/ ||
| AKTIN_BROKER_API_KEY | API key for the broker RESTful API with admin privileges  | xxxAdmin1234 ||

When using API-key authentication, please make sure that the broker server has a
corresponding API-key entry with `OU=admin` contained in the DN-string.



### Running the DSF Path

In order to run the backend using the DSF path, the following environment variables need to be set:

| EnvVar | Description | Example | Default |
|--------|-------------|---------|---------|
| DSF_SECURITY_CACERT | Certificate required for secured communication with the DSF middleware. |||
| DSF_SECURITY_KEYSTORE_P12FILE | Security archive (`PKCS #12`) carrying authentication information required for communication with the DSF middleware. |||
| DSF_SECURITY_KEYSTORE_PASSWORD | Password required to decrypt the security archive for subsequent use. |||
| DSF_WEBSERVICE_BASE_URL | Base URL pointing to the local ZARS FHIR server. | `https://zars/fhir` ||
| DSF_WEBSOCKET_URL | URL pointing to the local ZARS FHIR server websocket endpoint. | `wss://zars/fhir/ws` ||
| DSF_ORGANIZATION_ID | Identifier for the local organization this backend is part of. | `MY ZARS` ||

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

**Note:** _If you need the database to run using another port than 5432 then set the corresponding environment variable like so:_
```
FEASIBILITY_DATABASE_PORT=<your-desired-port> docker-compose up -d
```

### Test if container is running properly
```
GET http://localhost:8090/api/v1/terminology/root-entries
```

Should reply with status 200 and a JSON object
