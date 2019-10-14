package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.client.AddClientIdToTokenEndpointRequest;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

public class AddMTLSClientAuthenticationToTokenEndpointRequest extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

}
