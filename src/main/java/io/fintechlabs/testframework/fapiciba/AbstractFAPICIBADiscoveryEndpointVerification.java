package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckBackchannelUserCodeParameterSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscBackchannelAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointBackchannelAuthenticationRequestSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.FAPICIBACheckDiscEndpointGrantTypesSupported;
import io.fintechlabs.testframework.fapi.AbstractFAPIDiscoveryEndpointVerification;

public abstract class AbstractFAPICIBADiscoveryEndpointVerification extends AbstractFAPIDiscoveryEndpointVerification {

	protected abstract void performProfileSpecificChecks();

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		performEndpointVerification();

		callAndContinueOnFailure(CheckDiscBackchannelAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA-4");
		callAndContinueOnFailure(CheckDiscEndpointBackchannelAuthenticationRequestSigningAlgValuesSupported.class, Condition.ConditionResult.WARNING, "CIBA-4");
		callAndContinueOnFailure(CheckBackchannelUserCodeParameterSupported.class, Condition.ConditionResult.WARNING, "CIBA-4");
		callAndContinueOnFailure(FAPICIBACheckDiscEndpointGrantTypesSupported.class, Condition.ConditionResult.FAILURE, "CIBA-4");

		performProfileSpecificChecks();

		fireTestFinished();
	}


}
