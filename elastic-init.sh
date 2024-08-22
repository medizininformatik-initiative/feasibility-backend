#!/bin/sh

# Wait for Elasticsearch to start up before doing anything
until curl -X GET "$ELASTIC_HOST/_cluster/health" | grep -q '"status":"green"\|"status":"yellow"'; do
    echo "Waiting for Elasticsearch..."
    sleep 5
done

echo "Downloading $ELASTIC_FILEPATH$ELASTIC_FILENAME"
curl -sLO "$ELASTIC_FILEPATH$ELASTIC_FILENAME"

unzip -o $ELASTIC_FILENAME

if [ "$OVERRIDE_EXISTING" = "true" ]; then
  echo "(Trying to) delete existing indices"
  curl -s -DELETE "$ELASTIC_HOST/ontology"
  curl -s -DELETE "$ELASTIC_HOST/codeable_concept"
fi

echo "Creating ontology index..."
response_onto=$(curl --write-out "%{http_code}" -s --output /dev/null -XPUT -H 'Content-Type: application/json' "$ELASTIC_HOST/ontology" -d @elastic/ontology_index.json)
echo "Creating codeable concept index..."
response_cc=$(curl --write-out "%{http_code}" -s --output /dev/null -XPUT -H 'Content-Type: application/json' "$ELASTIC_HOST/codeable_concept" -d @elastic/codeable_concept_index.json)
echo "Done"

for FILE in elastic/*; do
  if [ -f "$FILE" ]; then
    BASENAME=$(basename "$FILE")
    if [[ $BASENAME == onto_es__ontology* && $BASENAME == *.json ]]; then
      if [[ "$response_onto" -eq 200 || "$OVERRIDE_EXISTING" = "true" ]]; then
        echo "Uploading $BASENAME"
        curl -s --output /dev/null -XPOST -H 'Content-Type: application/json' --data-binary @"$FILE" "$ELASTIC_HOST/ontology/_bulk?pretty"
      else
        echo "Skipping $BASENAME because index was already existing. Set OVERRIDE_EXISTING to true to force creating a new index"
      fi
    fi
    if [[ $BASENAME == onto_es__codeable_concept* && $BASENAME == *.json ]]; then
      if [[ "$response_cc" -eq 200 || "$OVERRIDE_EXISTING" = "true" ]]; then
        echo "Uploading $BASENAME"
        sed -i 's/valuesets/value_sets/g' "$FILE"
        curl -s --output /dev/null -XPOST -H 'Content-Type: application/json' --data-binary @"$FILE" "$ELASTIC_HOST/codeable_concept/_bulk?pretty"
      else
        echo "Skipping $BASENAME because index was already existing. Set OVERRIDE_EXISTING to true to force creating a new index"
      fi
    fi
  fi
done

echo "All done"
