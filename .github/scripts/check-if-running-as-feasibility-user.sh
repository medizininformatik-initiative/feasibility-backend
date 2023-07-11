#!/bin/bash -e

if docker exec -u0 feasibility-gui-backend pgrep -u feasibility java > /dev/null
then
    echo "Java process is running as feasibility"
    exit 0
else
    echo "Java process is not running as feasibility"
    exit 1
fi
