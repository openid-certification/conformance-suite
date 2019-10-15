package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddAccountRequestIdToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.OpenBankingUkAddMultipleAcrClaimsToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OpenBankingUkAuthorizationEndpointSetup extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddAccountRequestIdToAuthorizationEndpointRequest.class);
		callAndStopOnFailure(OpenBankingUkAddMultipleAcrClaimsToAuthorizationEndpointRequest.class);
	}

}
