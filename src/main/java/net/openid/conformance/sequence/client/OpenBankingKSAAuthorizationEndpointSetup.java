package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.OpenBankingKSAAddIndentIdToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OpenBankingKSAAuthorizationEndpointSetup extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(OpenBankingKSAAddIndentIdToAuthorizationEndpointRequest.class);
	}

}
