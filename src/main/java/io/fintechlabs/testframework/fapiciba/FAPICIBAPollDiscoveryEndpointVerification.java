package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckBackchannelTokenDeliveryPollModeSupported;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-poll-discovery-end-point-verification",
	displayName = "FAPI-CIBA: Poll Mode Discovery Endpoint Verification",
	summary = "This test ensures that the server's configurations (including token_delivery_modes, response_types, grant_types etc) is containing the required value in the specification",
	profile = "FAPI-CIBA",
	configurationFields = {
		"server.discoveryUrl",
	}
)
public class FAPICIBAPollDiscoveryEndpointVerification extends AbstractFAPICIBADiscoveryEndpointVerification {

	@Override
	public void performProfileSpecificChecks() {

		callAndContinueOnFailure(CheckBackchannelTokenDeliveryPollModeSupported.class, Condition.ConditionResult.FAILURE, "CIBA-4");
	}

}
