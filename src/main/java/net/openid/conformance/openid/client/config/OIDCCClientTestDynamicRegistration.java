package net.openid.conformance.openid.client.config;

import net.openid.conformance.openid.client.AbstractOIDCCClientTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "oidcc-client-test-dynamic-registration",
	displayName = "OIDCC: Relying party dynamic registration test",
	summary = "The client is expected to register using the registration endpoint." +
		"Corresponds to rp-registration-dynamic test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
@VariantNotApplicable(parameter = ClientRegistration.class, values = {"static_client"})
public class OIDCCClientTestDynamicRegistration extends AbstractOIDCCClientTest {
	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if(receivedRegistrationRequest) {
			fireTestFinished();
			return true;
		}
		return false;
	}
}
