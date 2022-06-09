package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddResourceUrlToConfig;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "payments-api-dcr-test-unauthorized-client",
	displayName = "Payments API Attempt to use payments with unauthorized client",
	summary = "Obtain a software statement from the Brazil sandbox directory (using a hardcoded client that has the DADOS role), verify (in several different ways) that it is not possible to obtain a client with the 'payments' scope granted, and that a client credentials grant requesting the 'payments' scope fails with an 'invalid_scope' error. Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl"
	}
)
public class PaymentsApiDcrTestModuleUnauthorizedClient extends AbstractApiDcrTestModuleUnauthorizedClient {

	protected void setupResourceEndpoint() {
		callAndStopOnFailure(AddResourceUrlToConfig.class);
		super.setupResourceEndpoint();
	}
	@Override
	boolean isPaymentsApiTest() {
		return true;
	}
}
