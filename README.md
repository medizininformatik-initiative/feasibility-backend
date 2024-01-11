# MII Feasibility Backend


## Configuration Base

| EnvVar                                   | Description                                                                                                                                                            | Example          | Default                                          |
|------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|--------------------------------------------------|
| LOG_LEVEL                                | Sets the log level being used. Possible values are: `error`, `warn`, `info`, `debug` and `trace`.                                                                      |                  | `warn`                                           |
| HIBERNATE_SHOW_SQL                      | Show the sql statements hibernate executes                                                                                                                             |                  | `false`                                          |
| BROKER_CLIENT_MOCK_ENABLED               | Enables the mock client. Possible values are `true` and `false`.                                                                                                       |                  | `true`                                           |
| BROKER_CLIENT_DIRECT_ENABLED             | Enables the direct client. Possible values are `true` and `false`.                                                                                                     |                  | `false`                                          |
| BROKER_CLIENT_AKTIN_ENABLED              | Enables the aktin client. Possible values are `true` and `false`.                                                                                                      |                  | `false`                                          |
| BROKER_CLIENT_DSF_ENABLED                | Enables the dsf client. Possible values are `true` and `false`.                                                                                                        |                  | `false`                                          |
| KEYCLOAK_BASE_URL                        | Base URL of the keycloak instance.                                                                                                                                     |                  | `http://localhost:8080`                          |
| KEYCLOAK_BASE_URL_ISSUER                 | Base URL the keycloak instance uses in the issuer claim                                                                                                                |                  | `http://localhost:8080`                          |
| KEYCLOAK_BASE_URL_JWK                    | Base URL for the JWK Set URI of the keycloak instance                                                                                                                  |                  | `http://localhost:8080`                          |
| KEYCLOAK_REALM                           | Realm to be used for checking bearer tokens.                                                                                                                           |                  | `feasibility`                                    |
| KEYCLOAK_CLIENT_ID                       | Client ID to be used for checking bearer tokens.                                                                                                                       |                  | `feasibility-webapp`                             |
| KEYCLOAK_ALLOWED_ROLE                    | Role that has to be part of the bearer token in order for the requester to be authorized.                                                                              |                  | `FeasibilityUser`                                |
| KEYCLOAK_POWER_ROLE                      | Optional role that can be assigned to a user to free them from being subject to any hard limits (see _PRIVACY_QUOTA_HARD.*_ EnvVars).                                  |                  | `FeasibilityPowerUser`                           |
| KEYCLOAK_ADMIN_ROLE                      | Role that gives admin rights to a user. Admins do not fall under any limits and can also see un-obfuscated site names.                                                 |                  | `FeasibilityAdmin`                               |
| SPRING_DATASOURCE_URL                    | The JDBC URL of the Postgres feasibility database.                                                                                                                     |                  | `jdbc:postgresql://feasibility-db:5432/codex_ui` |
| SPRING_DATASOURCE_USERNAME               | Username to connect to the Postgres feasibility database.                                                                                                              |                  | `guidbuser`                                      |
| SPRING_DATASOURCE_PASSWORD               | Password to connect to the Postgres feasibility database.                                                                                                              |                  | `guidbpw`                                        |
| ONTOLOGY_FILES_FOLDER_UI                 |                                                                                                                                                                        |                  | ontology/ui_profiles                             |
| ONTOLOGY_DB_MIGRATION_FOLDER             |                                                                                                                                                                        |                  | ontology/migration                               |
| MAPPINGS_FILE                            |                                                                                                                                                                        |                  | ontology/termCodeMapping.json                    |
| CONCEPT_TREE_FILE                        |                                                                                                                                                                        |                  | ontology/conceptTree.json                        |
| CQL_TRANSLATE_ENABLED                    |                                                                                                                                                                        |                  | true                                             |
| FHIR_TRANSLATE_ENABLED                   |                                                                                                                                                                        |                  | false                                            |
| FLARE_WEBSERVICE_BASE_URL                | URL of the local FLARE webservice - needed for FHIR query translation and when running the DIRECT path                                                                 |                  | http://localhost:5000                            |
| CQL_SERVER_BASE_URL                      | URL of the local FHIR server that handles CQL requests                                                                                                                 |                  | http://cql                                       |
| API_BASE_URL                             | Sets the base URL of the webservice. This is necessary if the webservice is running behind a proxy server. If not filled, the API base URL is the request URL          | https://host/api |                                                  |
| QUERY_VALIDATION_ENABLED                 | When enabled, any structured query submitted via the `run-query` endpoint is validated against the JSON schema located in `src/main/resources/query/query-schema.json` | true / false     | true                                             |
| QUERYRESULT_EXPIRY_MINUTES               | How many minutes should query results be kept in memory?                                                                                                               |                  | 5                                                |
| QUERYRESULT_PUBLIC_KEY                   | The public key in Base64-encoded DER format without banners and line breaks. Mandatory if _QUERYRESULT_DISABLE_LOG_FILE_ENCRYPTION_ is _false_                         |
| QUERYRESULT_DISABLE_LOG_FILE_ENCRYPTION  | Disable encryption of the result log file.                                                                                                                             | true / false     |                                                  |                                                                         
| ALLOWED_ORIGINS                          | Allowed origins for cross-origin requests. This should at least cover the frontend address.                                                                            |                  | http://localhost                                 |
| MAX_SAVED_QUERIES_PER_USER              | How many slots does a user have to store saved queries.                                                                                                                |                  | 10                                               |


### Running the DIRECT Path

The DIRECT path can be run **either** with FLARE **or** with a CQL compatible server, not with both.
Result counts from the direct path can be obfuscated for privacy reasons. The current implementation
handles obfuscation by adding or subtracting a random number <=5.

| EnvVar                                    | Description                                                                    | Example            | Default |
|-------------------------------------------|--------------------------------------------------------------------------------|--------------------|---------|
| BROKER_CLIENT_DIRECT_AUTH_BASIC_USERNAME  | Username to use to connect to flare or directly to the FHIR server via CQL     | feas-user          |         |
| BROKER_CLIENT_DIRECT_AUTH_BASIC_PASSWORD  | Password for that user                                                         | verysecurepassword |         |
| BROKER_CLIENT_DIRECT_USE_CQL              | Whether to use a CQL server or not.                                            |                    | false   |
| BROKER_CLIENT_OBFUSCATE_RESULT_COUNT      | Whether the result counts retrieved from the direct broker shall be obfuscated |                    | false   |

This is irrelevant if _BROKER_CLIENT_DIRECT_ENABLED_ is set to false.

#### Running the DIRECT Path with Local FLARE

In order to run the backend using the DIRECT broker path with FLARE,
the _FLARE_WEBSERVICE_BASE_URL_ environment variable needs to be set to a running instance of a FLARE
instance the backend is allowed to connect to.

#### Running the DIRECT Path with Local CQL Server

In order to run the backend using the DIRECT broker path with CQL,
the _CQL_SERVER_BASE_URL_ environment variable needs to be set to a running instance of a CQL compatible
FHIR server.


### Running the AKTIN Broker Path

In order to run the backend using the AKTIN broker path, the following environment variables need to be set:

| EnvVar                | Description                                              | Example                       | Default |
|-----------------------|----------------------------------------------------------|-------------------------------|---------|
| AKTIN_BROKER_BASE_URL | Base URL for the AKTIN RESTful API                       | http://localhost:8080/broker/ |         |
| AKTIN_BROKER_API_KEY  | API key for the broker RESTful API with admin privileges | xxxAdmin1234                  |         |

When using API-key authentication, please make sure that the broker server has a
corresponding API-key entry with `OU=admin` contained in the DN-string.



### Running the DSF Path

In order to run the backend using the DSF path, the following environment variables need to be set:

| EnvVar                         | Description                                                                                                           | Example              | Default |
|--------------------------------|-----------------------------------------------------------------------------------------------------------------------|----------------------|---------|
| DSF_SECURITY_CACERT            | Certificate required for secured communication with the DSF middleware.                                               |                      |         |
| DSF_SECURITY_KEYSTORE_P12FILE  | Security archive (`PKCS #12`) carrying authentication information required for communication with the DSF middleware. |                      |         |
| DSF_SECURITY_KEYSTORE_PASSWORD | Password required to decrypt the security archive for subsequent use.                                                 |                      |         |
| DSF_PROXY_HOST                 | Proxy host to be used.                                                                                                |                      |         |
| DSF_PROXY_USERNAME             | Proxy username to be used.                                                                                            |                      |         |
| DSF_PROXY_PASSWORD             | Proxy password to be used.                                                                                            |                      |         |
| DSF_WEBSERVICE_BASE_URL        | Base URL pointing to the local ZARS FHIR server.                                                                      | `https://zars/fhir`  |         |
| DSF_WEBSOCKET_URL              | URL pointing to the local ZARS FHIR server websocket endpoint.                                                        | `wss://zars/fhir/ws` |         |
| DSF_ORGANIZATION_ID            | Identifier for the local organization this backend is part of.                                                        | `MY ZARS`            |         |


### Privacy and Obfuscation

In order to prevent potentially malicious attempts to obtain critical patient data, several
countermeasures have been implemented. Users are restricted to creating a certain amount of queries per timeframe.
Permanently pushing this limit will get a user blacklisted and needs manual intervention if the user shall be de-listed again.
Moreover, retrieving detailed results for queries (including a breakdown by (obfuscated) site names) is also limited.

If the number of responding sites is below the configured threshold, only the total number of results will be provided.
If the number of total results is below threshold, no result will be provided.


| EnvVar                                                        | Description                                                                                                                                                   | Example | Default |
|---------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|---------|
| PRIVACY_QUOTA_SOFT_CREATE_AMOUNT                              | Amount of queries a user can create in the interval defined in _PRIVACY_QUOTA_SOFT_CREATE_INTERVALMINUTES_.                                                   |         | 3       |
| PRIVACY_QUOTA_SOFT_CREATE_INTERVALMINUTES                     | (see description above)                                                                                                                                       |         | 1       |
| PRIVACY_QUOTA_HARD_CREATE_AMOUNT                              | Amount of queries a user can create in the interval defined in _PRIVACY_QUOTA_HARD_CREATE_INTERVALMINUTES_ before being blacklisted.                          |         | 50      |
| PRIVACY_QUOTA_HARD_CREATE_INTERVALMINUTES                     | (see description above)                                                                                                                                       |         | 10080   |
| PRIVACY_QUOTA_READ_SUMMARY_POLLINGINTERVALSECONDS             | Interval in which a user can read the summary query result endpoint.                                                                                          |         | 10      |
| PRIVACY_QUOTA_READ_DETAILED_OBFUSCATED_POLLINGINTERVALSECONDS | Interval in which a user can read the detailed obfuscated query result endpoint.                                                                              |         | 10      |
| PRIVACY_QUOTA_READ_DETAILED_OBFUSCATED_AMOUNT                 | Amount of times a user can create a distinct detailed obfuscated result in the interval defined in _PRIVACY_QUOTA_READ_DETAILED_OBFUSCATED_INTERVALSECONDS _. |         | 10      |
| PRIVACY_QUOTA_READ_DETAILED_OBFUSCATED_INTERVALSECONDS        | (see description above)                                                                                                                                       |         | 3       |
| PRIVACY_THRESHOLD_RESULTS                                     | If the total number of results is below this number, return an empty result instead.                                                                          |         | 3       |
| PRIVACY_THRESHOLD_SITES                                       | If the number of responding sites (above PRIVACY_THRESHOLD_SITES_RESULT) is below this number, only respond with a total amount of patients                   |         | 20      |
| PRIVACY_THRESHOLD_SITES_RESULT                                | Any site that reports a number below this threshold is considered as non-responding (or zero) in regard to PRIVACY_THRESHOLD_SITES                            |         | 20      |

## Support for self-signed certificates

The feasibility backend supports the use of self-signed certificates from your own CAs.
On each startup, the feasibility backend will search through the folder /app/certs inside the container, add all found
CA *.pem files to a java truststore and start the application with this truststore.

Using docker-compose, mount a folder from your host (e.g.: ./certs) to the /app/certs folder,
add your *.pem files (one for each CA you would like to support) to the folder and ensure that they
have the .pem extension.

## Setting up Development

In order to run this project the following steps need to be followed:

1. Add GitHub package repositories
2. Build the project
3. Setup database


### Adding GitHub Package Repositories

This project uses dependencies ([HiGHmed DSF](https://github.com/highmed/highmed-dsf) and [sq2cql](https://github.com/medizininformatik-initiative/sq2cql)) which are not hosted on maven central but on GitHub.

In order to download artifacts from GitHub package repositories, you need to add your GitHub login credentials to your central maven config file.

For more information take a look at this GitHub documentation about [authentication](https://docs.github.com/en/free-pro-team@latest/packages/using-github-packages-with-your-projects-ecosystem/configuring-apache-maven-for-use-with-github-packages#authenticating-to-github-packages).

In order to install the packages using maven in your own projects you need a personal GitHub access token. This [GitHub documentation](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/creating-a-personal-access-token) shows you how to generate one.

After that, add the following `<server>` configurations to the `<servers>` section in your local _.m2/settings.xml_. Replace `USERNAME` with your GitHub username and `TOKEN` with the previously generated personal GitHub access token. The token needs at least the scope `read:packages`.

```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>codex</id>
      <username>USERNAME</username>
      <password>TOKEN</password>
    </server>
    <server>
      <id>highmed</id>
      <username>USERNAME</username>
      <password>TOKEN</password>
    </server>
  </servers>
</settings>
```

### Building the Project

Since Release 4.0.0, this project is packaged without ontology files. They have to be downloaded first from the corresponding
[GitHub repository](https://github.com/medizininformatik-initiative/fhir-ontology-generator). This can be done by enabling
the maven profile "download-ontology" when building the project. This wipes any existing ontology files in your project.
So if you are working with your own ontology files, do **not** execute this.

Navigate to the root of this repository and execute `mvn install -Pdownload-ontology` (or omit the -Pdownload-ontology part
when working with your own).

You can change your run configuration in intellij to execute maven goals before running. So if you want to always just
grab the latest ontology from GitHub, you can Edit your run configuration, go to `modify options` and select `add before launch task`
and `run maven goal` with `clean package -Pdownload-ontology`. This is however not necessary, and your mileage may vary
in other IDEs if they offer such an option.

Be aware that Step 1 "Add GitHub package repositories" needs to be executed before.

### Setting up the Database

The project requires a PSQL database. The easiest way to set this up is to use the docker-compose file provided:

`docker-compose up -d`

Note that this starts an empty psql database as well as a containerized version of the backend.
The containerized version of the backend will then connect to the backend database.
One can then connect to the same database when starting the backend in an IDE.

## Working with the Backend

This backend provides a rest webservice which connects the [MII feasibility gui](https://github.com/medizininformatik-initiative/feasibility-gui)
and the corresponding middlewares.

To send a query to the backend, use the following example query:

```
curl --location --request POST 'http://localhost:8090/api/v3/query' \
--header 'Content-Type: application/json' \
--data-raw '{
    "version": "http://to_be_decided.com/draft-1/schema#",
    "display": "",
    "inclusionCriteria": [
      [
        {
          "termCode": {
            "code": "29463-7",
            "system": "http://loinc.org",
            "version": "v1",
            "display": "Body Weight"
        },
        "valueFilter": {
            "type": "quantity-comparator",
            "unit": {
              "code": "kg",
              "display": "kilogram"
            },
            "comparator": "gt",
            "value": 90
          }
        }
      ]
    ]
  }'
```
another example
```
curl --location --request POST 'http://localhost:8090/api/v3/query' \
--header 'Content-Type: application/json' \
--data-raw '{
    "version": "http://to_be_decided.com/draft-1/schema#",
    "display": "xxx",
    "inclusionCriteria": [
      [
        {
          "termCode": {
            "code": "J98.4",
            "system": "urn:oid:1.2.276.0.76.5.409",
            "version": "v1",
            "display": "xxx"
        }

        }
      ]
    ]
  }'
```


The response to this call will return a location header, which links to the endpoint where the result
for the query can be collected with one of the available sub-paths.
For a full description of the api, please refer to the swagger documentation (either in static/v3/api-docs/swagger.yaml
or at http://localhost:8090/swagger-ui/index.html when running)


## Starting with Docker

### Creating the Docker Image
```
mvn install
docker build -t feasibility-gui-backend .
```

### Starting the Backend and the Database
```
docker-compose up -d
```

**Note:** _If you need the database to run using another port than 5432 then set the corresponding environment variable like:_
```
FEASIBILITY_DATABASE_PORT=<your-desired-port> docker-compose up -d
```

### Testing if the Container is Running Properly
```
GET http://localhost:8090/api/v3/terminology/root-entries
```

Should reply with status 200 and a JSON object

## Query Result Log Encryption

### Generating a Public/Private Key Pair

According to [BSI TR-02102-1][1], we have to use RSA keys with a minimum size of 3000 bit. We will use 3072 because that is the next possible value.

Generate the private key:

```sh
openssl genrsa -out key.pem 3072
```

Extract the public key from the private key in Base64-encoded DER format to put into `QUERYRESULT_PUBLIC_KEY`:

```sh
openssl rsa -in key.pem -outform DER -pubout | base64
```

If you like to use the `Decryptor` class, you have to convert the private key into the [PKCS#8](https://www.rfc-editor.org/rfc/rfc5208) format:

```sh
openssl pkcs8 -topk8 -inform PEM -outform DER -in key.pem -nocrypt | base64
```

You can use the following Java code to create a `PrivateKey` class for use with `Decryptor`:

```java
var keyFactory = KeyFactory.getInstance("RSA");
var privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode("...")));
```

[1]: <https://www.bsi.bund.de/DE/Themen/Unternehmen-und-Organisationen/Standards-und-Zertifizierung/Technische-Richtlinien/TR-nach-Thema-sortiert/tr02102/tr02102_node.html>
