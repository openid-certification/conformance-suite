# GOV.UK One Login fork of the OIDC Conformance Suite

This fork extends the OpenID Foundation conformance suite with custom test plans for GOV.UK One Login.

These are based on existing OIDC and FAPI2 standards, with modifications to support the GOV.UK One Login profile.

## Prerequisites

- Maven
- Docker

## Running locally

For full instructions on how to run the suite, see instructions below in the [wiki for the main project](https://gitlab.com/openid/conformance-suite/wikis/home).

Quickstart on a developer mac:
- `mvn clean package -Dmaven.test.skip` (build the application, skipping tests for speed)
- `docker compose -f docker-compose-dev-mac.yml up` (run the application)

The application runs at https://localhost:8443 (you may need to ignore a self-signed certificate warning).

## GOV.UK test plans

These can be found in the [net.openid.conformance.govuk](src/main/java/net/openid/conformance/govuk) package,
and each requires a different client configuration.

### GOV.UK One Login basic test plan

Select the plan under `GOV.UK` -> `GOV.UK One Login: Basic plan based on the OIDC Core Basic Certification Profile`

You will need to select an authentication method, and configure a corresponding client in your target environment.
Only two variants are currently supported:
- `client_secret_post` - requires configuring the client secret in `client.client_secret`
- `private_key_jwt` - requires configuring a static signing key in `client.jwks`

To run against a local orchestration-api you will need to provide configuration matching the clients configured in local-running.
Example JSON configurations can be found in [scripts/test-configs-govuk/](scripts/test-configs-govuk/).

# OpenID Foundation conformance suite

![OIDF logo](https://gitlab.com/openid/conformance-suite/-/raw/master/src/main/resources/static/images/openid.png?ref_type=heads)

This is the OpenID Foundation conformance suite, which covers OpenID Connect,
FAPI1-Advanced, FAPI2, FAPI-CIBA and OpenID for Identity Assurance (ekyc).

The project is located at https://gitlab.com/openid/conformance-suite
and all issues / merge requests should be submitted there.

See the wiki for more info: https://gitlab.com/openid/conformance-suite/wikis/home

For user instructions for testing / certifying please see:

https://openid.net/certification/instructions/

To get started as a developer or running the conformance suite locally, see:

https://gitlab.com/openid/conformance-suite/wikis/Developers/Build-&-Run

For guidelines for submitting contributions, see:

https://gitlab.com/openid/conformance-suite/wikis/Developers/Contributing

OpenID Foundation would like to thank the follow people/organisations:

* OpenBanking Ltd, for donating the FAPI conformance suite that formed
the basis of this code

* Office of the National Coordinator for Health Information
Technology, for donating the prototype code that formed the basis of
the FAPI conformance suite and some further enhancements.

* Authlete, for providing ongoing access to test environments compliant with
OpenID Connect, FAPI-R, FAPI-RW, FAPI1, FAPI2, FAPI-CIBA, PAR and DPoP specifications.

* Authlete, for various contributions of code including additional
PAR and FAPI tests

* Kumar Jayanti, for contributing the initial PAR versions of the FAPI-RW tests
