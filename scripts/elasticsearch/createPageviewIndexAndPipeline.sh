#!/bin/sh

HOST=$1
INDEX=$2

if [ -z "$HOST" ]
then
  echo "Usage: createPageviewIndexAndPipeline.sh <host> <index>"
  exit 1
fi

# Index name defaults to "pageview"
if [ -z "$INDEX" ]
then
  INDEX="pageview"
fi

# Set default port to 9200
if ! echo "$HOST" | grep -qE ':[0-9]+$'
then
  HOST="$HOST:9200"
fi

# Make sure host starts with https?://
if ! echo "$HOST" | grep -qE '^https?://.*'
then
  HOST="http://$HOST"
fi

curl -XPUT "$HOST/$INDEX?pretty" -H 'Content-Type: application/json' -d \
'{
  "mappings": {
    "properties": {
      "datetime": {
        "type": "date"
      },
      "geo": {
        "properties": {
          "location": { "type": "geo_point" }
        }
      }
    }
  }
}'

curl -XPUT "$HOST/_ingest/pipeline/pageview?pretty" -H 'Content-type: application/json' -d \
'{
  "description" : "Enrich pageview entry",
  "processors" : [
    {
      "geoip" : {
        "field": "ip",
        "target_field": "geo",
        "properties": [
            "continent_name",
            "country_iso_code",
            "country_name",
            "region_iso_code",
            "region_name",
            "city_name",
            "location"
        ]
      }
    },
    {
      "remove": {
        "field": "ip"
      }
    },
    {
      "user_agent" : {
        "field" : "ua",
        "target_field": "ua_parsed"
      }
    },
    {
      "uri_parts": {
        "field": "url",
        "target_field": "url_parsed",
        "keep_original": false
      }
    },
    {
      "uri_parts": {
        "field": "referrer",
        "target_field": "referrer_parsed",
        "keep_original": false
      }
    }
  ]
}'