package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddInvalidIssValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-federation-client-invalid-iss-in-id-token",
	displayName = "OpenID Federation RP test: Invalid iss in id token",
	summary = "The test deliberately inserts an invalid iss into the id token, " +
		"which must be detected and rejected by the RP.",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationClientInvalidIssInIdTokenTest extends OpenIDFederationClientTest {

	@Override
	protected void beforeSigningIdToken() {
		callAndContinueOnFailure(AddInvalidIssValueToIdToken.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.7-2");
	}

}
