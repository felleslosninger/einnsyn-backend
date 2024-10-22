# Authentication

This folder contains all configuration related to authentication. The application has two kinds of authentication:

## API key
API keys are used by public bodies to publish and manage their own data. An API key is linked to one organization, and when authenticated using an API key, you have access to everything owned by that organization, and sub-organizations.

API key authentication is done by adding a request header named `X-EIN-API-KEY` containing the key:

```
curl -H "X-EIN-API-KEY: secret_..." https://api.einnsyn.no/
```

## Username/password
Users can create private accounts where they can handle things like access requests and saved searches. You can authenticate as a user using Oauth 2.0:

```
curl -X POST https://api.einnsyn.no/auth/token \
     -H "Content-Type: application/json" \
     -d '{"username": "user@example.com", "password": "password"}'
```

```
curl -X GET https://api.einnsyn.no \
     -H "Authorization: Bearer access_token"
```

## Ansattporten
In the future, employees will be able to authenticate and send requests on behalf of a public body using [Ansattporten](https://docs.digdir.no/docs/idporten/oidc/ansattporten_guide.html).
