package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckBackchannelTokenDeliveryPingModeSupported;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-ping-discovery-end-point-verification",
	displayName = "FAPI-CIBA: Ping Mode Discovery Endpoint Verification",
	summary = "This test ensures that the server's configurations (including token_delivery_modes, response_types, grant_types etc) is containing the required value in the specification",
	profile = "FAPI-CIBA",
	configurationFields = {
		"server.discoveryUrl",
	}
)
public class FAPICIBAPingDiscoveryEndpointVerification extends AbstractFAPICIBADiscoveryEndpointVerification {

	@Override
	public void performProfileSpecificChecks() {

		callAndContinueOnFailure(CheckBackchannelTokenDeliveryPingModeSupported.class, Condition.ConditionResult.FAILURE, "CIBA-4");
	}

}
