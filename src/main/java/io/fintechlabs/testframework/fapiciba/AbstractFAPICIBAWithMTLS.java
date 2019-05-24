package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;

public abstract class AbstractFAPICIBAWithMTLS extends AbstractFAPICIBA {

	@Override
	protected void createClientAssertionSteps() {

		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);

	}

}
