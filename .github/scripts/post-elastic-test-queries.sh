#!/bin/bash -e

curl "$ELASTIC_HOST/_cat/indices"

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

echo "All elastic search tests completed"
exit 0