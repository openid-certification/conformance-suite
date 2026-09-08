# Open Finance Brazil CIBA migration notes

These changes apply to the `openbanking_brazil` CIBA profile. Other CIBA
profiles keep their existing configuration requirements.

## Client (RP) tests: notification TLS credentials

All Brazil CIBA client tests now require `mtls.cert` and `mtls.key` in the
test configuration. The suite uses these credentials as the emulated
authorization server when it calls the client's notification endpoint.
The `mtls.ca` certificate chain remains optional.

Before you reuse a saved configuration, add the certificate and private key
under **TLS certificates for client (used to make MTLS connections)**. The
JSON field names are `mtls.cert`, `mtls.key`, and, if needed, `mtls.ca`.
Without the required credentials, setup stops before the test starts.
Use test credentials that the notification endpoint accepts, not production keys.

The new `fapi-ciba-id1-client-ping-without-mtls-certificate-test` deliberately
omits this certificate for its negative notification request. It still needs
the normal configuration so that the missing certificate is the only intended
transport change.

This implements the notification endpoint mTLS requirement in
[Open Finance Brazil CIBA 2.1.0 beta2, section 6.3.4](https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/2092204111).

## Server (OP) tests: registered notification endpoint

Brazil server tests now expose `notification_uri` on the suite's mTLS host
under `/test-mtls/`, instead of the regular host under `/test/`.

For a statically registered client, update `backchannel_client_notification_endpoint`
at the authorization server to the exact `notification_uri` shown by the new
test run. Do this before approving the authentication request. An old
registration that points to `/test/` will no longer reach the Brazil notification
handler. Dynamic registration sends the new URI automatically.

For a self-hosted suite, set `fintechlabs.base_mtls_url` to the publicly reachable
mTLS origin. Its proxy must request client certificates and forward the trusted
certificate header to the suite. The regular `fintechlabs.external_url_override`
setting (including `EXTERNAL_URL` in the development profile) does not apply to
mTLS notifications. There is no separate mTLS override: configure the public mTLS
origin through `fintechlabs.base_mtls_url`. A tunnel that only serves the regular
HTTP endpoint is not sufficient for these tests.
