package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.client.WaitFor30Seconds;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-attempt-reuse-authorisation-code-after-30seconds",
	displayName = "FAPI-RW-ID2: try to reuse authorization code after one second",
	summary = "This test tries reusing an authorization code after 30 seconds, as the authorization code has already been used this must fail with the AS returning an invalid_grant error. As per rfc6749 section 4.1.2: 'If an authorization code is used more than once, the authorization server MUST deny the request and SHOULD revoke (when possible) all tokens previously issued based on that authorization code.' - if the access token is not revoked, the suite will raise a warning - warnings do not prevent you certifying.",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPIRWID2AttemptReuseAuthorizationCodeAfter30S extends AbstractFAPIRWID2AttemptReuseAuthorizationCode {

	@Override
	protected void waitForAmountOfTime() {
		callAndStopOnFailure(WaitFor30Seconds.class);
	}
}
