package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.client.AddAcrClaimToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

public class FAPIAuthorizationEndpointSetup extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddAcrClaimToAuthorizationEndpointRequest.class);
	}

}
