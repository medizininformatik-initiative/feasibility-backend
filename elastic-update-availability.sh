#!/usr/bin/env bash
set -e

BASE_DIR="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 || exit 1 ; pwd -P )"
ES_BASE_URL=${ES_BASE_URL:-http://localhost:9200}
ES_INDEX=${ES_INDEX:-ontology}

FILES=("$BASE_DIR"/update-availability/*)
for availUpdateBundle in "${FILES[@]}"; do
  echo "Sending Availability Update bundle $availUpdateBundle ..."
  curl -X POST -H "Content-Type: application/json" --data-binary @"$availUpdateBundle" "$ES_BASE_URL/$ES_INDEX/_bulk"
done
