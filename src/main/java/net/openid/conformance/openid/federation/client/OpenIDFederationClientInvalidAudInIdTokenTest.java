package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddInvalidAudValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-federation-client-invalid-aud-in-id-token",
	displayName = "openid-federation-client-invalid-aud-in-id-token",
	summary = "openid-federation-client-invalid-aud-in-id-token",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationClientInvalidAudInIdTokenTest extends OpenIDFederationClientTest {

	@Override
	protected void beforeSigningIdToken() {
		callAndContinueOnFailure(AddInvalidAudValueToIdToken.class, Condition.ConditionResult.FAILURE);
	}

}
