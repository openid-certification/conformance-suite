package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.authzen.condition.EnsureAuthzenApiResponseHasWwwAuthenticate;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.PDPAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-invalid-credentials-returns-401",
	displayName = "AuthZEN Subject Search API - Section 11.3: Invalid credentials returns 401",
	summary = "Per Section 11.3, when client authentication is required and the supplied credentials are invalid, the PDP MUST return HTTP 401. The response SHOULD include a WWW-Authenticate header (surfaced as a warning). Under mtls the client certificate has the configured subject and issuer but is signed by a new key, so no CA vouches for it; the PDP may instead refuse the connection in the TLS handshake, which also passes.",
	profile = "AuthZEN"
)
@VariantNotApplicable(parameter = PDPAuthType.class, values = {"none"})
public class AuthzenPDPSubjectSearchInvalidCredentialsReturns401Test extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected boolean corruptAuthCredentials() {
		return true;
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 401;
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (!pdpRejectedAtTlsLayer()) {
			callAndContinueOnFailure(EnsureAuthzenApiResponseHasWwwAuthenticate.class, ConditionResult.WARNING, "AUTHZEN-11.3");
		}
		super.onPostAuthorizationFlowComplete();
	}
}
