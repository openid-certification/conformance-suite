package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddAccountRequestIdToKSAAuthorizationEndpointRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OpenBankingKSAAuthorizationEndpointSetup extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddAccountRequestIdToKSAAuthorizationEndpointRequest.class);
	}

}
