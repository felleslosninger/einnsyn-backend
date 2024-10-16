# <img src="https://einnsyn.no/8ebf89f8e40d3eb75183.svg" width="180px" alt="eInnsyn"/>

[![Maven build status](https://github.com/felleslosninger/ein-api-experimental/actions/workflows/call-maventests.yml/badge.svg)](https://github.com/felleslosninger/ein-api-experimental/actions/workflows/call-maventests.yml)
[![Build image](https://github.com/felleslosninger/ein-api-experimental/actions/workflows/call-buildimage.yml/badge.svg)](https://github.com/felleslosninger/ein-api-experimental/actions/workflows/call-buildimage.yml)

[eInnsyn](https://einnsyn.no) is a web service that aims to make the Norwegian public sector more open and accessible, by providing a web interface for the public to access documents from various public bodies. eInnsyn is developed by [the Norwegian Digitalisation Agency](https://www.digdir.no/digdir/about-norwegian-digitalisation-agency/887).

This application serves as the core back-end for eInnsyn. The main functionalities of the application include:

- **Public API**: The application exposes a well-defined RESTful API that enables both public and internal clients to access and interact with stored data. This API provides endpoints for searching documents based on multiple criteria, retrieving document metadata, as well as uploading data. Our [API specification](https://github.com/felleslosninger/ein-openapi) is described in a separate repository.

- **Authentication**: The application has two levels of authentication: one for public bodies to publish and manage their own data, and one for users to handle their own account and related things like access requests and saved searches. Public bodies can authenticate using an API key, and users authenticate using username / password. In the future, people will be able to authenticate and send requests on behalf of a public body using [Ansattporten](https://docs.digdir.no/docs/idporten/oidc/ansattporten_guide.html).

- **Data reception**: The API accepts JSON-formatted data according to our [API specification](https://github.com/felleslosninger/ein-openapi). Authenticity of the publisher is checked, and the data structure is validated before saving.

- **Access requests**: Users can request access to protected documents through the API. The requests will be sent to the owning entity, either by e-mail or over [eFormidling](https://samarbeid.digdir.no/eformidling/dette-er-eformidling/46). The entities can also browse access requests directed to themselves through the API.

- **Saved searches**: Users can save searches and cases, and request to be notified when there are matching updates, through the API.

## Project structure

- [authentication/](src/main/java/no/einnsyn/apiv3/authentication/): Everything related to authentication, like security filters and authentication services.
- [common/](src/main/java/no/einnsyn/apiv3/common/): Classes that are used / extended by several entities.
- [configuration/](src/main/java/no/einnsyn/apiv3/configuration/): Configuration of various services.
- [entities/](src/main/java/no/einnsyn/apiv3/entities/): Services, models, DTOs etc., grouped by the entities in our data model.
- [error/](src/main/java/no/einnsyn/apiv3/error/): Exception / Error handling.
- [tasks/](src/main/java/no/einnsyn/apiv3/tasks/): Tasks that are run outside of the main request flow, like cron jobs or events.
- [utils/](src/main/java/no/einnsyn/apiv3/utils/): Utility classes and services.
- [validation/](src/main/java/no/einnsyn/apiv3/validation/): Validators for our data model.

## Installation

The application requires an elasticsearch index with mappings defined in [scripts/elasticsearch](scripts/elasticsearch/). The PostgreSQL schema is defined in Flyway migrations under [src/main/resources/db/migration](src/main/resources/db/migration).

### Requirements

- JDK 21
- Maven

## Usage

The following services are required to run the application:

- Elasticsearch
- PostgreSQL
- E-mail server

### Environment variables (and their default values)

```
# Application settings
BASE_URL=http://localhost:8080
ROOT_API_KEY=

# PostgreSQL settings
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost/arkiv
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=

# Elasticsearch settings
ELASTICSEARCH_URIS=http://localhost:9200
ELASTICSEARCH_ACTIVE_INDEX=test

# Email settings
EMAIL_SMTP=localhost
EMAIL_PORT=25
EMAIL_USER=
EMAIL_PASS=
EMAIL_SMTP_AUTH=false
EMAIL_SMTP_PROTOCOL=smtp
EMAIL_FROM=eInnsyn.no <test@example.com>
EMAIL_FROM_HOST=example.com

# Authentication settings
JWT_SECRET=

# Elasticsearch reindex settings
ELASTICSEARCH_CONCURRENCY=10
ELASTICSEARCH_REINDEXER_CRON_UPDATE=0 */10 * * * *
ELASTICSEARCH_REINDEXER_CRON_REMOVE=0 0 0 * * *
ELASTICSEARCH_REINDEXER_INDEX_BATCH_SIZE=25
ELASTICSEARCH_REINDEXER_SAKSMAPPE_SCHEMA_TIMESTAMP=
ELASTICSEARCH_REINDEXER_JOURNALPOST_SCHEMA_TIMESTAMP=
ELASTICSEARCH_REINDEXER_MOETEMAPPE_SCHEMA_TIMESTAMP=
ELASTICSEARCH_REINDEXER_MOETESAK_SCHEMA_TIMESTAMP=

# Innsynskrav settings
INNSYNSKRAV_DEBUG_RECIPIENT=

# eFormidling settings
MOVE_URL=
IP_ORGNUMMER=000000000

```

### Running the application

```
mvn spring-boot:run
```

## License

eInnsyn is Open Source software released under the [BSD-3-Clause license](LICENSE).
