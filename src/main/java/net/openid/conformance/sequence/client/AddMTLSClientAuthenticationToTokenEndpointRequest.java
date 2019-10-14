package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddClientIdToTokenEndpointRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class AddMTLSClientAuthenticationToTokenEndpointRequest extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddClientIdToTokenEndpointRequest.class);
	}

}
