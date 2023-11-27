# <img src="https://einnsyn.no/8ebf89f8e40d3eb75183.svg" width="180px" alt="eInnsyn"/>

[![Maven build status](https://github.com/felleslosninger/ein-api-experimental/actions/workflows/call-maventests.yml/badge.svg)](https://github.com/felleslosninger/ein-api-experimental/actions/workflows/call-maventests.yml)
[![Build image](https://github.com/felleslosninger/ein-api-experimental/actions/workflows/call-buildimage.yml/badge.svg)](https://github.com/felleslosninger/ein-api-experimental/actions/workflows/call-buildimage.yml)

[eInnsyn](https://einnsyn.no) is a search service that makes the Norwegian public sector more open and accessible.

This repository is a work-in-progress of a new back-end for the service. The application is responsible for receiving documents from public bodies, indexing them, and making them available through an API.

## Installation

The application requires an elasticsearch index with mappings defined in [scripts/elasticsearch](scripts/elasticsearch/). The PostgreSQL schema is defined in Flyway scripts under [src/main/resources/db/migration](src/main/resources/db/migration).

### Requirements

- JDK 21
- Maven

## Usage

### Environment variables (and their default values)

```
ELASTICSEARCH_URIS=http://localhost:9200
ELASTICSEARCH_ACTIVE_INDEX=test
SPRING_DATASOURCE_URL=localhost
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=
EMAIL_SMTP=localhost
EMAIL_PORT=25
EMAIL_USER=
EMAIL_PASS=
EMAIL_SMTP_AUTH=false
EMAIL_SMTP_STARTTLS_ENABLE=false
EMAIL_SMTP_STARTTLS_REQUIRED=false
BASE_URL=http://localhost:8080
```

### Runtime dependencies

This application requires the following services to run:

- Elasticsearch
- PostgreSQL

### Running the application

```
mvn spring-boot:run
```

## License

eInnsyn is Open Source software released under the [BSD-3-Clause license](LICENSE).
