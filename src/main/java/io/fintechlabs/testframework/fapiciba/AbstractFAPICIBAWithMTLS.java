package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddClientIdToBackchannelAuthenticationEndpointRequest;
import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.EnsureServerConfigurationSupportsMTLS;

public abstract class AbstractFAPICIBAWithMTLS extends AbstractFAPICIBA {

	@Override
	protected void addClientAuthenticationToBackchannelRequest() {
		callAndContinueOnFailure(EnsureServerConfigurationSupportsMTLS.class, Condition.ConditionResult.FAILURE, "FAPI-RW-5.2.2-6");

		callAndStopOnFailure(AddClientIdToBackchannelAuthenticationEndpointRequest.class);
	}

	@Override
	protected void addClientAuthenticationToTokenEndpointRequest() {
		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

}
