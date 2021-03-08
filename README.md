# CODEX Feasibility Backend

## Running the AKTIN broker path

In order to run the backend using the AKTIN broker path, the following environment variables need to be set:

| EnvVar | Description | Example |
|--------|-------------|---------|
| AKTIN_BROKER_BASE_URL | Base URL for the AKTIN RESTful API | http://localhost:8080/broker/ |
| AKTIN_BROKER_API_KEY | API key for the broker RESTful API with admin privileges  | xxxAdmin1234 |

When using API-key authentication, please make sure that the broker server has a corresponding the
corresponding API-key entry with `OU=admin` contained in the DN-string.


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
