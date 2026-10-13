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
	summary = "Per Section 11.3, when client authentication is required and the supplied credentials are invalid, the PDP MUST return HTTP 401. The response SHOULD include a WWW-Authenticate header (surfaced as a warning).",
	profile = "AuthZEN"
)
@VariantNotApplicable(parameter = PDPAuthType.class, values = {"none", "mtls"})
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
	protected String getExpectedSearchResponseJson() {
		return "{ \"results\": [] }";
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
		callAndContinueOnFailure(EnsureAuthzenApiResponseHasWwwAuthenticate.class, ConditionResult.WARNING, "AUTHZEN-11.3");
		super.onPostAuthorizationFlowComplete();
	}
}
