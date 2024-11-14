#!/bin/bash -e

ABSOLUTE_FILEPATH="${ELASTIC_FILEPATH//TAGPLACEHOLDER/$ONTOLOGY_GIT_TAG}$ELASTIC_FILENAME"
echo "Downloading $ABSOLUTE_FILEPATH"
response_onto_dl=$(curl --write-out "%{http_code}" -sLO "$ABSOLUTE_FILEPATH")

if [ "$response_onto_dl" -ne 200 ]; then
  echo "Could not download ontology file. Maybe the tag $ONTOLOGY_GIT_TAG does not exist? Error code was $response_onto_dl"
  exit 1
fi

unzip -o "$ELASTIC_FILENAME"

echo "(Trying to) delete existing indices"
curl --request DELETE "$ELASTIC_HOST/ontology"
curl --request DELETE "$ELASTIC_HOST/codeable_concept"

echo "Creating ontology index..."
response_onto=$(curl --write-out "%{http_code}" -s --output /dev/null -XPUT -H 'Content-Type: application/json' "$ELASTIC_HOST/ontology" -d @elastic/ontology_index.json)
echo "Creating codeable concept index..."
response_cc=$(curl --write-out "%{http_code}" -s --output /dev/null -XPUT -H 'Content-Type: application/json' "$ELASTIC_HOST/codeable_concept" -d @elastic/codeable_concept_index.json)
echo "Done."

for FILE in elastic/*; do
  if [ -f "$FILE" ]; then
    BASENAME=$(basename "$FILE")
    if [[ $BASENAME == onto_es__ontology* && $BASENAME == *.json ]]; then
      if [[ "$response_onto" -eq 200 || "$OVERRIDE_EXISTING" = "true" ]]; then
        echo "Uploading $BASENAME"
        curl -s --output /dev/null -XPOST -H 'Content-Type: application/json' --data-binary @"$FILE" "$ELASTIC_HOST/ontology/_bulk"
      else
        echo "Skipping $BASENAME because index was already existing. Set OVERRIDE_EXISTING to true to force creating a new index"
      fi
    fi
    if [[ $BASENAME == onto_es__codeable_concept* && $BASENAME == *.json ]]; then
      if [[ "$response_cc" -eq 200 || "$OVERRIDE_EXISTING" = "true" ]]; then
        echo "Uploading $BASENAME"
        curl -s --output /dev/null -XPOST -H 'Content-Type: application/json' --data-binary @"$FILE" "$ELASTIC_HOST/codeable_concept/_bulk"
      else
        echo "Skipping $BASENAME because index was already existing. Set OVERRIDE_EXISTING to true to force creating a new index"
      fi
    fi
  fi
done

echo "All done"
