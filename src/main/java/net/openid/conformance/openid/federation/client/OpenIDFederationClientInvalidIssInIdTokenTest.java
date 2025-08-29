package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddInvalidIssValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-federation-client-invalid-iss-in-id-token",
	displayName = "openid-federation-client-invalid-iss-in-id-token",
	summary = "openid-federation-client-invalid-iss-in-id-token",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationClientInvalidIssInIdTokenTest extends OpenIDFederationClientTest {

	@Override
	protected void beforeSigningIdToken() {
		callAndContinueOnFailure(AddInvalidIssValueToIdToken.class, Condition.ConditionResult.FAILURE);
	}

}
