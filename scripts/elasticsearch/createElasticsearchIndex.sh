#!/bin/sh

HOST=$1
INDEX=$2

if [ -z "$HOST" ] || [ -z "$INDEX" ]
then
  echo "Usage: createElasticsearchIndex.sh <host> <index>"
  exit 1
fi

# Make sure host starts with https?://
if ! [[ $HOST =~ ^https?://.* ]]
then
  HOST="http://$HOST"
fi

# Set default port to 9200
if ! [[ $HOST =~ :[0-9]+ ]]
then
  HOST="$HOST:9200"
fi

URL="$HOST/$INDEX"

# Create mapping
curl -X PUT "$URL?pretty" -H 'Content-Type: application/json' -d'
{
  "settings": {
    "index": {
      "routing": {
        "allocation": {
          "include": {
            "_tier_preference": "data_content"
          }
        }
      },
      "number_of_shards": "3",
      "analysis": {
        "filter": {
          "remove_leading_zeros_filter": {
            "pattern": "0*(\\d+)",
            "type": "pattern_replace",
            "replacement": "$1"
          },
          "normalize_two_components_id_filter": {
            "pattern": "^(\\d+)\\W(\\d+)$",
            "type": "pattern_replace",
            "replacement": "$1/$2"
          },
          "unique_filter": {
            "type": "unique",
            "only_on_same_position": "true"
          },
          "no_stem_filter": {
            "type": "stemmer",
            "language": "norwegian"
          },
          "id_wildcard_pattern_capture_filter": {
            "type": "pattern_capture",
            "preserve_original": "false",
            "patterns": [
              "^\\*+\\W\\*\\W(\\d+)$"
            ]
          },
          "no_synonym_filter": {
            "type": "synonym",
            "synonyms_path": "analysis/synonym.txt"
          },
          "normalize_three_components_id_filter": {
            "pattern": "^(\\d+)\\W(\\d+)\\W(\\d+)$",
            "type": "pattern_replace",
            "replacement": "$1/$2-$3"
          },
          "autocomplete_filter_4_25": {
            "type": "edge_ngram",
            "min_gram": "4",
            "max_gram": "25"
          }
        },

        // Is this needed? Should we match quotes?
        "char_filter": {
          "handle_special_char_filter": {
            "type": "mapping",
            "mappings": [
              "\" =>",
              "‘ =>",
              "’ =>",
              "“ =>",
              "” =>",
              "« =>",
              "» =>",
              ", => \\u0020",
              ". => \\u0020"
            ]
          }
        },
        "normalizer": {
          "keyword_lowercase": {
            "filter": [
              "lowercase"
            ],
            "type": "custom"
          }
        },
        "analyzer": {
          "id_analyzer": {
            "filter": [
              "remove_leading_zeros_filter",
              "normalize_two_components_id_filter",
              "normalize_three_components_id_filter"
            ],
            "type": "custom",
            "tokenizer": "whitespace"
          },
          "id_search_analyzer": {
            "filter": [
              "remove_leading_zeros_filter",
              "normalize_two_components_id_filter",
              "normalize_three_components_id_filter",
              "id_wildcard_pattern_capture_filter"
            ],
            "type": "custom",
            "tokenizer": "whitespace"
          },
          "lower_reverse": {
            "filter": [
              "lowercase",
              "reverse",
              "autocomplete_filter_4_25"
            ],
            "type": "custom",
            "tokenizer": "whitespace"
          },
          "no_analyzer": {
            "filter": [
              "lowercase",
              "no_synonym_filter",
              "keyword_repeat",
              "no_stem_filter",
              "unique_filter"
            ],
            "char_filter": [
              "icu_normalizer",
              "html_strip",
              "handle_special_char_filter"
            ],
            "type": "custom",
            "tokenizer": "whitespace"
          }
        }
      },
      "number_of_replicas": "1"
    }
  },
  
  "mappings": {
    "dynamic": "false",
    "properties": {
      "arkivskaperNavn": {
        "type": "text",
        "index": false,
        "copy_to": [
          "search_innhold"
        ]
      },
      "arkivskaperSorteringNavn": {
        "type": "keyword",
        "normalizer": "keyword_lowercase"
      },
      "arkivskaperTransitive": {
        "type": "keyword",
        "eager_global_ordinals": true
      },
      "avsender": {
        "type": "text",
        "analyzer": "no_analyzer"
      },
      "avsender_SENSITIV": {
        "type": "text",
        "analyzer": "no_analyzer"
      },
      "dokumentbeskrivelse": {
        "properties": {
          "tittel": {
            "type": "text",
            "index": false,
            "copy_to": [
              "search_innhold"
            ]
          },
          "tittel_SENSITIV": {
            "type": "text",
            "index": false,
            "copy_to": [
              "search_innhold_SENSITIV"
            ]
          }
        }
      },
      "dokumentetsDato": {
        "type": "date"
      },
      "journaldato": {
        "type": "date"
      },
      "journalpostnummer": {
        "type": "keyword",
        "copy_to": [
          "search_id",
          "journalpostnummer_sort"
        ]
      },
      "journalpostnummer_sort": {
        "type": "integer"
      },
      "journalposttype": {
        "type": "keyword"
      },
      "korrespondansepart": {
        "properties": {
          "korrespondansepartNavn": {
            "type": "text",
            "copy_to": [
              "search_innhold",
              "search_korrespodansepart_sort"
            ],
            "analyzer": "no_analyzer"
          },
          "korrespondansepartNavn_SENSITIV": {
            "type": "text",
            "copy_to": [
              "search_innhold_SENSITIV"
            ],
            "analyzer": "no_analyzer"
          }
        }
      },
      "moetedato": {
        "type": "date"
      },
      "mottaker": {
        "type": "text",
        "analyzer": "no_analyzer"
      },
      "mottaker_SENSITIV": {
        "type": "text",
        "analyzer": "no_analyzer"
      },
      "møtesakssekvensnummer": {
        "type": "keyword",
        "copy_to": [
          "search_id",
          "search_sakssekvensnummer",
          "sakssekvensnummer_sort"
        ]
      },
      "møtesaksår": {
        "type": "keyword",
        "copy_to": [
          "search_id",
          "search_saksaar",
          "saksaar_sort"
        ]
      },
      "offentligTittel": {
        "type": "text",
        "index": false,
        "copy_to": [
          "search_tittel",
          "search_tittel_prefix"
        ]
      },
      "offentligTittel_SENSITIV": {
        "type": "text",
        "index": false,
        "copy_to": [
          "search_tittel_SENSITIV",
          "search_tittel_sort"
        ]
      },
      "opprettetDato": {
        "type": "date"
      },
      "parent": {
        "properties": {
          "offentligTittel": {
            "type": "text",
            "index": false,
            "copy_to": [
              "search_innhold"
            ]
          },
          "offentligTittel_SENSITIV": {
            "type": "text",
            "index": false,
            "copy_to": [
              "search_innhold_SENSITIV"
            ]
          },
          "saksaar": {
            "type": "keyword",
            "index": false,
            "copy_to": [
              "search_saksaar",
              "saksaar_sort"
            ]
          },
          "saksnummer": {
            "type": "keyword",
            "index": false,
            "copy_to": [
              "search_id"
            ]
          },
          "saksnummerGenerert": {
            "type": "keyword",
            "index": false,
            "copy_to": [
              "search_id"
            ]
          },
          "sakssekvensnummer": {
            "type": "keyword",
            "index": false,
            "copy_to": [
              "search_sakssekvensnummer",
              "sakssekvensnummer_sort"
            ]
          }
        }
      },
      "publisertDato": {
        "type": "date"
      },
      "queries": {
        "properties": {
          "query": {
            "type": "percolator"
          }
        }
      },
      "registreringsID": {
        "type": "keyword",
        "index": false,
        "copy_to": [
          "search_id"
        ]
      },
      "saksaar": {
        "type": "keyword",
        "index": false,
        "copy_to": [
          "search_saksaar",
          "saksaar_sort"
        ]
      },
      "saksaar_sort": {
        "type": "integer"
      },
      "saksnummer": {
        "type": "keyword",
        "index": false,
        "copy_to": [
          "search_id"
        ]
      },
      "saksnummerGenerert": {
        "type": "keyword",
        "index": false,
        "copy_to": [
          "search_id"
        ]
      },
      "sakssekvensnummer": {
        "type": "keyword",
        "index": false,
        "copy_to": [
          "search_sakssekvensnummer",
          "sakssekvensnummer_sort"
        ]
      },
      "sakssekvensnummer_sort": {
        "type": "integer"
      },
      "search_id": {
        "type": "text",
        "analyzer": "id_analyzer",
        "search_analyzer": "id_search_analyzer"
      },
      "search_innhold": {
        "type": "text",
        "analyzer": "no_analyzer"
      },
      "search_innhold_SENSITIV": {
        "type": "text",
        "analyzer": "no_analyzer"
      },
      "search_korrespodansepart_sort": {
        "type": "keyword",
        "normalizer": "keyword_lowercase"
      },
      "search_saksaar": {
        "type": "keyword"
      },
      "search_sakssekvensnummer": {
        "type": "keyword"
      },
      "search_tittel": {
        "type": "text",
        "analyzer": "no_analyzer"
      },
      "search_tittel_SENSITIV": {
        "type": "text",
        "analyzer": "no_analyzer"
      },
      "search_tittel_prefix": {
        "type": "text",
        "analyzer": "lower_reverse"
      },
      "search_tittel_sort": {
        "type": "keyword",
        "normalizer": "keyword_lowercase"
      },
      "skjerming": {
        "properties": {
          "skjermingshjemmel": {
            "type": "text",
            "copy_to": [
              "search_innhold",
              "search_innhold_SENSITIV"
            ],
            "analyzer": "no_analyzer"
          }
        }
      },
      "standardDato": {
        "type": "date"
      },
      "type": {
        "type": "keyword",
        "eager_global_ordinals": true
      },
      "created": {
        "type": "date"
      },
      "updated": {
        "type": "date"
      }
    }
  }
}'
