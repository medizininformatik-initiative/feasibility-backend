#!/bin/bash -e

URL=$1

RESPONSE=$(curl -s -w "\n%{http_code}" "$URL")
HTTP_STATUS=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_STATUS" -ne 200 ]; then
    echo "Error: URL did not return HTTP 200. Status: $HTTP_STATUS"
    exit 1
fi

if ! echo "$BODY" | jq . >/dev/null 2>&1; then
    echo "Error: Response is not valid JSON."
    exit 1
fi

ONTOLOGY_TAG_ENTRY=$(echo "$BODY" | jq -r '.terminology.ontologyTag // empty')
if [[ -z "$ONTOLOGY_TAG_ENTRY" ]]; then
    echo "Error: JSON does not contain key 'terminology.ontologyTag'."
    exit 1
fi

if [[ "$ONTOLOGY_TAG_ENTRY" != v* ]]; then
    echo "Error: Value of 'terminology.ontologyTag' does not start with 'v'. Found: $ONTOLOGY_TAG_ENTRY"
    exit 1
fi

# If all checks pass
echo "Success: URL is reachable, returns JSON, and 'ontology-tag' starts with 'v'."
exit 0
