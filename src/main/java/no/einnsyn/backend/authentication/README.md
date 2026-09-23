# Authentication

This folder contains all configuration related to authentication. The application has two kinds of authentication:

## API key
API keys are used by public bodies to publish and manage their own data. An API key is linked to one organization, and when authenticated using an API key, you have access to everything owned by that organization, and sub-organizations.

API key authentication is done by adding a request header named `API-KEY` containing the key:

```
curl -H "API-KEY: secret_..." https://api.einnsyn.no/
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
Employees can authenticate on behalf of a public body using [Ansattporten](https://docs.digdir.no/docs/idporten/oidc/ansattporten_guide.html). The token's authorized orgnummer is resolved to an Enhet, which gives the same access as an API key for that Enhet.

If no verified Enhet exists for the orgnummer, the principal carries only the orgnummer. Such a principal can self-register an Enhet under a top node (when `application.ansattporten.allowSelfRegistration` is enabled), and can read and maintain that Enhet while it waits for verification. Unverified Enhets are invisible to everyone but admins and cannot publish data or hold API keys. An admin verifies by setting `verified: true` on the Enhet; a notification is sent to `application.enhet.verificationNotificationEmail` on self-registration.
