package net.openid.conformance.openid.client.config;

import net.openid.conformance.openid.client.AbstractOIDCCClientTest;
import net.openid.conformance.testmodule.PublishTestModule;


@PublishTestModule(
	testName = "oidcc-client-test-discovery-openid-config",
	displayName = "OIDCC: Relying party openid discovery test",
	summary = "The client is expected to retrieve and use the OpenID Provider Configuration Information." +
		"Corresponds to rp-discovery-openid-configuration test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestDiscoveryOpenIDConfiguration extends AbstractOIDCCClientTest {
	@Override
	protected boolean finishTestIfAllRequestsAreReceived() {
		if(receivedDiscoveryRequest) {
			fireTestFinished();
			return true;
		}
		return super.finishTestIfAllRequestsAreReceived();
	}
}
