package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.authzen.condition.EnsureAuthzenApiResponseHasWwwAuthenticate;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.PDPAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-missing-credentials-returns-401",
	displayName = "AuthZEN Resource Search API - Section 11.3: Missing credentials returns 401",
	summary = "Per Section 11.3, when client authentication is required and credentials are missing, the PDP MUST return HTTP 401. The response SHOULD include a WWW-Authenticate header (surfaced as a warning).",
	profile = "AuthZEN"
)
@VariantNotApplicable(parameter = PDPAuthType.class, values = {"none", "mtls"})
public class AuthzenPDPResourceSearchMissingCredentialsReturns401Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record" }
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
	protected boolean skipAuthentication() {
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
