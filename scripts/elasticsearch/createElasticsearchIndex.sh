#!/bin/sh

HOST=$1
ALIAS_NAME=$2

if [ -z "$HOST" ] || [ -z "$ALIAS_NAME" ]
then
  echo "Usage: createElasticsearchIndex.sh <host> <aliasName>"
  exit 1
fi

# Make sure host starts with https?://
if ! echo "$HOST" | grep -qE '^https?://.*'
then
  HOST="http://$HOST"
fi

# Set index name to arkiv-<timestamp>
INDEX_NAME="arkiv-$(date +%Y%m%d-%H%M)"

# Set default port to 9200
if ! echo "$HOST" | grep -qE ':[0-9]+$'
then
  HOST="$HOST:9200"
fi

# Create mapping
curl -X PUT "$HOST/$INDEX_NAME?pretty" -H 'Content-Type: application/json' -d @einnsynMapping.json

# Update alias
curl -X POST "$HOST/_aliases" -H 'Content-Type: application/json' -d "
{
    \"actions\": [
        {\"remove\": {\"index\": \"*\", \"alias\": \"$ALIAS_NAME\"}},
        {\"add\": {\"index\": \"$INDEX_NAME\", \"alias\": \"$ALIAS_NAME\"}}
    ]
}" > /dev/null
