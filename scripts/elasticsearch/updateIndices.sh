#!/usr/bin/env bash

#
# Script to update Elasticsearch index settings and mappings with minimal downtime.
# Usage: ES_USERNAME=username ES_PASSWORD=password ES_URL=http://localhost:9200 ./updateIndices.sh
#

set -eu

# Reads ES_USERNAME and ES_PASSWORD from environment
: "${ES_USERNAME:?ES_USERNAME must be set}"
: "${ES_PASSWORD:?ES_PASSWORD must be set}"

ES_URL="${ES_URL:-http://localhost:9200}"
AUTH="${ES_USERNAME}:${ES_PASSWORD}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="${SCRIPT_DIR}/../.."
MAPPINGS_FILE="${PROJECT_DIR}/src/main/resources/elasticsearch/indexMappings.json"
SETTINGS_FILE="${PROJECT_DIR}/src/main/resources/elasticsearch/indexSettings.json"

# Read list of aliases to ALIASES variable, default to "arkiv,percolator_queries"
IFS=',' read -ra ALIASES <<< "${ES_ALIASES:-arkiv,percolator_queries}"

# Extract dynamic settings (can be applied on an open index)
dynamic_settings=$(jq '{index: {number_of_replicas: .index.number_of_replicas, routing: .index.routing}}' "$SETTINGS_FILE")

# Extract static settings (requires close/open) — strip number_of_shards and other immutable settings
static_settings=$(jq '{index: {analysis: .index.analysis}}' "$SETTINGS_FILE")

for alias in "${ALIASES[@]}"; do
  echo "Updating alias: ${alias}"

  # 1. Apply dynamic settings (no downtime)
  echo "- Applying dynamic settings..."
  curl -s --fail-with-body -u "$AUTH" -X PUT "${ES_URL}/${alias}/_settings" \
    -H "Content-Type: application/json" \
    -d "$dynamic_settings"
  echo ""

  # 2. Close -> apply static settings (analysis) -> open (minimal downtime)
  echo "- Closing index..."
  curl -s --fail-with-body -u "$AUTH" -X POST "${ES_URL}/${alias}/_close"
  echo ""

  echo "- Applying static settings (analysis)..."
  curl -s --fail-with-body -u "$AUTH" -X PUT "${ES_URL}/${alias}/_settings" \
    -H "Content-Type: application/json" \
    -d "$static_settings"
  echo ""

  echo "- Reopening index..."
  curl -s --fail-with-body -u "$AUTH" -X POST "${ES_URL}/${alias}/_open"
  echo ""

  # 3. Update mappings (no downtime, but requires analyzers from settings)
  echo "- Updating mappings..."
  curl -s --fail-with-body -u "$AUTH" -X PUT "${ES_URL}/${alias}/_mapping" \
    -H "Content-Type: application/json" \
    -d @"$MAPPINGS_FILE"
  echo ""

  echo "Done: ${alias}"
  echo ""
done

echo "All indices updated."
