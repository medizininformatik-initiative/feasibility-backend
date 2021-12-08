#!/usr/bin/env bash

# Args:
#   1: directory to search certificates in

echo "Looking for certificates to import in '$1'..."

if [ -z "$(ls -A -- "$1")" ]; then
  echo "No files found within '$1'. Trust store is left unchanged."
else
  echo "Found files within '$1'. Trying to import them into the trust store..."

    for f in "$1"*.*; do
      echo -n "Importing file '$f'"...
      keytool -importcert -alias "${f##*/}" -storepass changeit -keystore cacerts -noprompt -file $f 2>&1

      if [ $? -eq 0 ]; then
        echo "DONE"
      else
        echo "FAILED"
      fi
    done
fi

echo "Starting main application..."

java -jar feasibility-gui-backend.jar
