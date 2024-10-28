#!/bin/bash -e

curl "$ELASTIC_HOST/_cat/indices"

response=$(curl -s -w "%{http_code}" -o response_body "$ELASTIC_HOST/ontology/_doc/9c2328b0-ac4e-3d69-8f2f-d8b905875348")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq '.found == true' | grep -q true; then
        echo "Ontology Document found in elastic search"
    else
        echo "Empty or nonexistent response from elastic search"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi


response=$(curl -s -w "%{http_code}" -o response_body "$ELASTIC_HOST/codeable_concept/_doc/d676be36-7f34-3ea6-9838-c0c9e1ca3dcc")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq '.found == true' | grep -q true; then
        echo "Codeable Concept Document found in elastic search"
    else
        echo "Empty or nonexistent response from elastic search"
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
        echo "OK response with non-empty array"
    else
        echo "Empty or nonexistent response"
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
        echo "OK response with non-empty array"
    else
        echo "Empty or nonexistent response"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi

response=$(curl -s -w "%{http_code}" --header "Authorization: Bearer $access_token" -o response_body "http://localhost:8091/api/v4/codeable-concept/entry/d676be36-7f34-3ea6-9838-c0c9e1ca3dcc")
http_code="${response: -3}"
json_body=$(cat response_body)

if [ "$http_code" -eq 200 ]; then
    if echo "$json_body" | jq -e '.code and .code != ""' | grep -q true; then
        echo "OK response with non-empty array"
    else
        echo "Empty or nonexistent response"
        exit 1
    fi
else
    echo "Response code $http_code"
    exit 1
fi

echo "All elastic search tests completed"
exit 0