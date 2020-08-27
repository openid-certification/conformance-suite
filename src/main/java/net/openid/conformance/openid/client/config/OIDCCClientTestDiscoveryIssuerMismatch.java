package net.openid.conformance.openid.client.config;

import net.openid.conformance.condition.as.ChangeIssuerInServerConfigurationToBeInvalid;
import net.openid.conformance.openid.client.AbstractOIDCCClientTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;


@PublishTestModule(
	testName = "oidcc-client-test-discovery-issuer-mismatch",
	displayName = "OIDCC: Relying party OpenID discovery issuer mismatch",
	summary = "The client is expected to retrieve OpenID Provider Configuration Information " +
		"from the .well-known/openid-configuration endpoint " +
		"and detect that the issuer in the provider configuration does not match the one returned by WebFinger." +
		"Corresponds to rp-discovery-issuer-not-matching-config test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestDiscoveryIssuerMismatch extends AbstractOIDCCClientTest {

	@Override
	protected Object handleDiscoveryEndpointRequest() {
		callAndStopOnFailure(ChangeIssuerInServerConfigurationToBeInvalid.class);
		Object returnValue = super.handleDiscoveryEndpointRequest();
		startWaitingForTimeout();
		return returnValue;
	}

	@Override
	protected Object handleAuthorizationEndpointRequest(String requestId) {
		throw new TestFailureException(getId(), "The client is expected to detect the issuer mismatch and stop" +
			" the flow after fetching OpenID Provider configuration.");
	}
}
