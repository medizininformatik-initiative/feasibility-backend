#!/bin/bash -e

curl "$ELASTIC_HOST/_cat/indices"

onto_example_id=$(grep '"_id":' ./elastic/onto_es__ontology_* | awk -F'"_id": "' '{print $2}' | awk -F'"' '{print $1}' | head -n 1)
cc_example_id=$(grep '"_id":' ./elastic/onto_es__codeable_concept_* | awk -F'"_id": "' '{print $2}' | awk -F'"' '{print $1}' | head -n 1)

response=$(curl -s -w "%{http_code}" -o response_body "$ELASTIC_HOST/ontology/_doc/$onto_example_id")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq '.found == true' | grep -q true; then
        echo "Ontology Document with id $onto_example_id found in elastic search"
    else
        echo "Empty or nonexistent response from elastic search for ontology document with id $onto_example_id"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi


response=$(curl -s -w "%{http_code}" -o response_body "$ELASTIC_HOST/codeable_concept/_doc/$cc_example_id")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq '.found == true' | grep -q true; then
        echo "Codeable Concept Document with id $cc_example_id found in elastic search"
    else
        echo "Empty or nonexistent response from elastic search for codeable_concept document with id $cc_example_id"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi

access_token="$(curl -s --request POST \
  --url http://localhost:8083/auth/realms/dataportal/protocol/openid-connect/token \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data grant_type=password \
  --data client_id=dataportal-webapp \
  --data username=testuser \
  --data password=testpassword \
  --data scope=openid | jq '.access_token' | tr -d '"')"

response=$(curl -s -w "%{http_code}" --header "Authorization: Bearer $access_token" -o response_body "http://localhost:8091/api/v4/terminology/entry/search?searchterm=Blutdruck")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq '.totalHits > 0' | grep -q true; then
        echo "OK response with non-empty array on onto search with searchterm"
    else
        echo "Empty or nonexistent response on onto search with searchterm"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi

response=$(curl -s -w "%{http_code}" --header "Authorization: Bearer $access_token" -o response_body "http://localhost:8091/api/v4/terminology/entry/$onto_example_id")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq -e '.termcode and .termcode != ""' | grep -q true; then
        echo "OK response with non-empty array on get onto by id"
    else
        echo "Empty or nonexistent response on get onto by id"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi

response=$(curl -s -w "%{http_code}" --header "Authorization: Bearer $access_token" -o response_body "http://localhost:8091/api/v4/codeable-concept/entry/search?searchterm=Vectorcardiogram")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq '.totalHits > 0' | grep -q true; then
        echo "OK response with non-empty array on cc search with searchterm"
    else
        echo "Empty or nonexistent response on cc search with searchterm"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi

response=$(curl -s -w "%{http_code}" --header "Authorization: Bearer $access_token" -o response_body "http://localhost:8091/api/v4/codeable-concept/entry/$cc_example_id")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq -e '.termCode.code and .termCode.code != ""' | grep -q true; then
        echo "OK response with non-empty array on get cc by id"
    else
        echo "Empty or nonexistent response on get cc by id"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi

echo "All elastic search tests completed"
exit 0