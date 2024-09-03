#!/bin/bash -e

if docker exec -u0 dataportal-backend pgrep -u dataportal java > /dev/null
then
    echo "Java process is running as dataportal"
    exit 0
else
    echo "Java process is not running as dataportal"
    exit 1
fi
