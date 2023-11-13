#!/bin/bash -e

if docker exec -u0 feasibility-gui-backend pgrep -u 10001 java > /dev/null
then
    echo "Java process is running as 10001"
    exit 0
else
    echo "Java process is not running as 10001"
    exit 1
fi
