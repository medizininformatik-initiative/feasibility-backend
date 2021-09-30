# CODEX Feasibility Backend


## Configuration Base

| EnvVar | Description | Example | Default |
|--------|-------------|---------|---------|
|BROKER_CLIENT_TYPE|Selects the BorkerClient implementation to be used. Valid types are: `DSF`, `AKTIN`, `DIRECT` , `MOCK` | `DSF` | `MOCK` |
|KEYCLOAK_ENABLED| Enables Keycloak if set to true. Possible values are `true` and `false`. | | `true` |
|KEYCLOAK_BASE_URL| Base URL to reach a keycloak instance. | | `http://localhost:8080` |
|KEYCLOAK_REALM| Realm to be used for checking bearer tokens. | | `codex-develop` |
|KEYCLOAK_CLIENT_ID| Client ID to be used for checking bearer tokens. | | `middleware-broker` |
|KEYCLOAK_ALLOWED_ROLE| Role that has to be part of the bearer token in order for the requester to be rendered authorized. | | `CODEX_USER` |
|FEASIBILITY_DATABASE_HOST|Host under which the Postgres feasibility database can be reached.|localhost|`localhost`|
|FEASIBILITY_DATABASE_USER|Username to connect to the Postgres feasibility database.|codex-postgres|`codex-postgres`|
|FEASIBILITY_DATABASE_PASSWORD|Password to connect to the Postgres feasibility database.|codex-password|`codex-password`|
|ONTOLOGY_FILES_FOLDER_UI | | | ontology/ui_profiles |
|MAPPINGS_FILE | | | ontology/termCodeMapping.json |
|CONCEPT_TREE_FILE | | | ontology/conceptTree.json |
|CQL_TRANSLATE_ENABLED | | | true |
|FHIR_TRANSLATE_ENABLED | | | false |
|FLARE_WEBSERVICE_BASE_URL | Url of the local FLARE webservice - needed for fhir query translation and when running the DIRECT path | | http://localhost:5000 |
|API_BASE_URL| sets the base url of the webservice, this is necessary if the webservice is running behind a proxy server, if not filled the api base url is the request url|https://host/api||

### Running the DIRECT path with local flare

In order to run the backend using the DIRECT broker path,
the FLARE_WEBSERVICE_BASE_URL environment variable needs to be set to a running instance of a FLARE
instance the backend is allowed to connect to.


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
| DSF_PROXY_HOST | Proxy host to be used. |||
| DSF_PROXY_USERNAME | Proxy username to be used. |||
| DSF_PROXY_PASSWORD | Proxy password to be used. |||
| DSF_WEBSERVICE_BASE_URL | Base URL pointing to the local ZARS FHIR server. | `https://zars/fhir` ||
| DSF_WEBSOCKET_URL | URL pointing to the local ZARS FHIR server websocket endpoint. | `wss://zars/fhir/ws` ||
| DSF_ORGANIZATION_ID | Identifier for the local organization this backend is part of. | `MY ZARS` ||


## Setting up Development

In order to run this project the following steps need to be followed:

1. Add github package repositories
2. Build the project
3. Setup database


### Add github package repositories

This project uses dependencies (HiGHmed DSF and sq2cql), which are not hosted on maven central but instead on github.

In order to download artifacts from github package repositories you need to add your github login credentials to your central maven config file.

For more information take a look at this GitHub documentation about [authentication](https://docs.github.com/en/free-pro-team@latest/packages/using-github-packages-with-your-projects-ecosystem/configuring-apache-maven-for-use-with-github-packages#authenticating-to-github-packages).

In order to install the packages using Maven in your own projects you need a personal GitHub access token. This [GitHub documentation](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/creating-a-personal-access-token) shows you how to generate one.

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

### Build the project

Navigate to the root of this repository and execute `mvn install`.
Be aware that Step 1 "Add github package repositories" needs to be executed before.

### Setup database

The project requires a PSQL database. The easiest way to set this up is to use the docker-compose file provided:

`docker-compose up -d`

Note that this starts an empty psql database as well as a containerized version of the backend.
The containerized version of the backend will then connects to the backend database.
One can then connect to the same database when starting the backend in an IDE.

## Working with the backend

This backend provides a rest webservice which connects the num codex feasibility gui (https://github.com/num-codex/codex-feasibility-gui)
and the num codex middlewares.

To send a query to the backend use the following example query:

```
curl --location --request POST 'http://localhost:8090/api/v1/query-handler/run-query' \
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
curl --location --request POST 'http://localhost:8090/api/v1/query-handler/run-query' \
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


The result of this query will return a location header, which links to the endpoint where the result
for the query can be collected.


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
