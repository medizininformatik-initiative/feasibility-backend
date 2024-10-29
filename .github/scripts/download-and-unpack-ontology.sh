#!/bin/bash -e

mkdir --parents .github/integration-test/ontology/ui_profiles .github/integration-test/ontology/migration
curl -L https://github.com/medizininformatik-initiative/fhir-ontology-generator/raw/${ONTOLOGY_GIT_TAG}/example/fdpg-ontology/backend.zip -o .github/integration-test/ontology/backend.zip
unzip -jod .github/integration-test/ontology/ui_profiles/ .github/integration-test/ontology/backend.zip
mv .github/integration-test/ontology/ui_profiles/R__Load_latest_ui_profile.sql .github/integration-test/ontology/migration/
rm .github/integration-test/ontology/backend.zip
