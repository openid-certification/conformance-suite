package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddInvalidAudValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-federation-client-invalid-aud-in-id-token",
	displayName = "OpenID Federation client test: Invalid aud in id token",
	summary = "The test deliberately inserts an invalid aud into the id token, " +
		"which must be detected and rejected by the RP.",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationClientInvalidAudInIdTokenTest extends OpenIDFederationClientTest {

	@Override
	protected void beforeSigningIdToken() {
		callAndContinueOnFailure(AddInvalidAudValueToIdToken.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.7-3");
	}

}
