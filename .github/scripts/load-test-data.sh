#!/bin/bash -e

curl -sH 'Content-Type: application/fhir+json' \
  -d @.github/test-data/TestPatient.json \
  -o /dev/null \
  -w 'Result: %{response_code}\n' \
  http://localhost:8082/fhir
