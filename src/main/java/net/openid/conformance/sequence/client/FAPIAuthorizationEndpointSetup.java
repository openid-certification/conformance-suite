package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddAcrClaimToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class FAPIAuthorizationEndpointSetup extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddAcrClaimToAuthorizationEndpointRequest.class);
	}

}
