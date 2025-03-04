package net.openid.conformance.openid.federation.client;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-federation-client-happy-path",
	displayName = "openid-federation-client-happy-path",
	summary = "openid-federation-client-happy-path",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_identifier"
	}
)
public class OpenIDFederationClientHappyPathTest extends AbstractOpenIDFederationClientTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		validateEntityStatement();
		validateAbsenceOfMetadataPolicy();
		validateImmediateSuperiors();

		fireTestFinished();
	}

}
