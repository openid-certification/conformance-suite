package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddClientIdToRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class AddMTLSClientAuthenticationToRequest extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddClientIdToRequest.class);
	}

}
