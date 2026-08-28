# Upgrading: login moves to a single OpenID Provider

**This release contains a breaking change for anyone running their own instance of
the conformance suite.** The built-in Google and GitLab sign-in options have been
removed. Every deployment now authenticates its users against one OpenID Provider
("the IdP"), which you configure. An instance upgraded without the configuration
below will start, but nobody will be able to log in.

Instances hosted by the OpenID Foundation are unaffected in operation — they are
already pointed at the Foundation's IdP, which brokers the Google and GitLab
accounts users had before.

## Before you upgrade

You need an OpenID Provider and a client registered with it. Any spec-compliant
OP works; the suite uses only discovery, the authorization code flow, and (when
offered) RP-initiated logout.

Register a confidential client with:

| Setting | Value |
|---|---|
| Redirect URI | `<your base_url>/login/oauth2/code/idp` |
| Post-logout redirect URI | `<your base_url>/login.html?logout=true` |
| Grant type | `authorization_code` |
| Scopes | `openid`, `email` |
| Client authentication | `client_secret_post` (override with `OIDC_IDP_AUTHENTICATION`) |

`<your base_url>` is the value you pass as `BASE_URL` / `fintechlabs.base_url`,
for example `https://localhost.emobix.co.uk:8443`.

The IdP must issue:

- **`email`** — used as the principal name. A user without one cannot log in.
- **`roles`** — a JSON array of role names. A user is an administrator when it
  contains the role named by `OIDC_IDP_ADMIN_ROLE` (default `conformance-admin`).
  Users without it get ordinary access. If no user has the role, the instance has
  no administrators, so make sure at least one account carries it before you cut
  over.

## Configuration

All of these bind from environment variables through Spring's relaxed binding, so
no `-D` flags are needed.

| Variable | Default | Meaning |
|---|---|---|
| `OIDC_IDP_ISS` | `https://openid.net/auth` | Issuer URL. Discovery is fetched from `<iss>/.well-known/openid-configuration`. |
| `OIDC_IDP_CLIENTID` | `conformance-suite` | Client id. |
| `OIDC_IDP_SECRET` | `idp-secret` | Client secret. |
| `OIDC_IDP_ADMIN_ROLE` | `conformance-admin` | Role name that grants administrator rights. |
| `OIDC_IDP_AUTHENTICATION` | `client_secret_post` | Client authentication method. |
| `OIDC_IDP_ID_TOKEN_ALG` | `ES256` | The single JWS algorithm ID tokens are verified with. |
| `OIDC_IDP_ACCESS_TOKEN_ALGS` | `ES256,RS256` | Comma-separated JWS algorithms API bearer tokens are verified with. |

**The defaults point at the OpenID Foundation's IdP and will not work for you.**
Set at least `OIDC_IDP_ISS`, `OIDC_IDP_CLIENTID` and `OIDC_IDP_SECRET`.

The two algorithm settings are deliberately configuration rather than something
the suite infers. A discovery document advertises what an OP *can* sign with,
which is not the same as what it *does* sign with, and ID tokens and access
tokens are not necessarily signed with the same algorithm. If logins fail with a
signature error, or API bearer tokens are rejected, check these two first.

## Settings that were removed

Delete these; they are no longer read, and leaving them set has no effect.

| Removed | Replacement |
|---|---|
| `oidc.google.clientid` / `.secret` / `.iss` | `oidc.idp.clientid` / `.secret` / `.iss` |
| `oidc.gitlab.clientid` / `.secret` / `.iss` | as above — one registration replaces both |
| `oidc.admin.domains` | `OIDC_IDP_ADMIN_ROLE` in the `roles` claim |
| `oidc.admin.group`, `oidc.admin.issuer` | as above |
| `oidc.gitlab.admin-group-indicator-claims` | as above |
| `OIDC_GOOGLE_CLIENTID` / `_SECRET` env vars | `OIDC_IDP_CLIENTID` / `OIDC_IDP_SECRET` |
| `OIDC_GITLAB_CLIENTID` / `_SECRET` env vars | as above |

The `-Doidc.google.*` and `-Doidc.gitlab.*` flags in the `Dockerfile` entrypoint
went with them. They were redundant even before this change — relaxed binding
already resolved those properties from the environment.

## Deployment specifics

**Docker.** Pass the variables into the container; the entrypoint needs no
changes.

**Helm chart.** The chart reads the issuer from the `idpIss` value and the
credentials from a secret named `oidc-idp-credentials` with keys `clientid` and
`secret`:

```bash
kubectl create secret generic oidc-idp-credentials \
  --from-literal=clientid="$OIDC_IDP_CLIENTID" \
  --from-literal=secret="$OIDC_IDP_SECRET"

helm install ... --set idpIss="$OIDC_IDP_ISS"
```

`idpIss` defaults to the OpenID Foundation's IdP in `chart/values.yaml`, so you
must override it — the default is a working value, not a correct one for your
deployment. The secret was previously named `oidc-google-credentials` /
`oidc-gitlab-credentials`; those can be deleted after the upgrade.

**docker-compose.** The dev stacks run with `--fintechlabs.devmode=true` and
bypass login entirely, so they need no IdP configuration to come up.

## Existing data

The suite stores the owner of every test, plan, log entry, API token and saved
configuration as an `(issuer, subject)` pair. Changing identity provider changes
that pair, so records created before the switch would no longer be visible to
their owner.

For the Foundation's own migration this is handled automatically: its IdP brokers
the previous Google and GitLab accounts and reports the original issuer and
subject in `idp_iss` / `idp_sub` claims, and on first login the suite hands the
user's records over from the old identity to the new one.

**A self-hosted instance moving between providers will not get this**, because it
depends on the IdP emitting those two claims. If that applies to you, either have
your IdP emit `idp_iss` and `idp_sub` for migrated accounts, or update the `owner`
sub-documents in MongoDB yourself (collections `TEST_INFO`, `TEST_PLAN`,
`API_TOKEN`, `TEST_CONFIG`, and `testOwner` in `EVENT_LOG`).

### Private share links do not survive the change

Share links are signed tokens that carry the plan's owner at the time they were
issued, so once a plan's ownership moves, previously distributed links stop
resolving. They cannot be repaired or re-signed — the owner is inside a
signature, and the links are already in other people's hands. Generate and
distribute new links after the owning user's first login through the IdP. See the
class documentation on `AssetSharing` for the mechanism.
