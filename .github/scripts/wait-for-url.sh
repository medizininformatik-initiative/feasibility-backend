#!/bin/bash -e

URL=$1
START_EPOCH="$(date +"%s")"

eclipsed() {
  EPOCH="$(date +"%s")"
  echo $((EPOCH - START_EPOCH))
}

# wait at maximum 240 seconds
while [[ ($(eclipsed) -lt 240) && ("$(curl -s -o /dev/null -w '%{response_code}' "$URL")" != "200") ]]; do
  sleep 2
done

if [ $(eclipsed) -lt 240 ]; then
  exit 0
else
  exit 1
fi
