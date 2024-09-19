#!/usr/bin/env bash
set -e

BASE_DIR="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 || exit 1 ; pwd -P )"
ES_BASE_URL=${ES_BASE_URL:-http://localhost:9200}
ES_INDEX=${ES_INDEX:-ontology}

FILES=("$BASE_DIR"/update-availability/*)
for availUpdateBundle in "${FILES[@]}"; do
  if [[ $(basename "$availUpdateBundle") == *.json ]]; then
    echo "Sending Availability Update bundle $availUpdateBundle ..."
    response=$(curl --write-out "%{http_code}" -s --output /dev/null -XPOST -H 'Content-Type: application/json' --data-binary @"$availUpdateBundle" "$ES_BASE_URL/$ES_INDEX/_bulk")
    echo "$response"
  else
    echo "Skipping $availUpdateBundle (not a .json file)"
  fi
done
