#!/bin/bash -e

wget -O mapping.zip https://neowolke.mig-frankfurt.de/s/qLkEp6NeYoCFcdE/download
unzip mapping.zip
mv mapping/*.json .github/integration-test/ontology/
