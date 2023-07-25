#!/bin/bash -e

access_token="$(curl -s --request POST \
  --url http://localhost:8083/auth/realms/feasibility/protocol/openid-connect/token \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data grant_type=password \
  --data client_id=feasibility-webapp \
  --data username=testuser \
  --data password=testpassword \
  --data scope=openid | jq '.access_token' | tr -d '"')"

response=$(curl -s -i \
  --url http://localhost:8091/api/v3/query \
  --header "Authorization: Bearer $access_token" \
  --header 'Content-Type: application/json' \
  --data '{
	"version": "http://to_be_decided.com/draft-1/schema#",
	"inclusionCriteria": [
		[
			{
				"termCodes": [
					{
						"code": "gender",
						"system": "mii.abide",
						"display": "Geschlecht"
					}
				],
				"attributeFilters": [
					{
						"type": "concept",
						"selectedConcepts": [
							{
								"code": "male",
								"system": "http://hl7.org/fhir/administrative-gender",
								"display": "Male"
							}
						],
						"attributeCode": {
							"code": "gender",
							"system": "mii.abide",
							"display": "Geschlecht"
						}
					}
				]
			}
		]
	]
}')

result_location=$(echo "$response" | grep -i location | awk '{print $2}')

nr_of_pats=$(curl -v \
  --url "${result_location%?}/summary-result" \
  --header "Authorization: Bearer $access_token" | jq '.totalNumberOfPatients')

echo "nr of pats: $nr_of_pats"

if [[ -n $nr_of_pats ]] && [[ $nr_of_pats -gt 0 ]]; then
  echo "result found and > 0"
  exit 0
else
  echo "result was empty or zero"
  exit 1
fi
