package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddAccountRequestIdToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.OpenBankingUkAddMultipleAcrClaimsToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class FAPI1AdvOpenBankingUkAuthorizationEndpointSetup extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddAccountRequestIdToAuthorizationEndpointRequest.class);
		// spec says acr is not required anymore so only send acr for first client
		call(condition(OpenBankingUkAddMultipleAcrClaimsToAuthorizationEndpointRequest.class).skipIfStringMissing("is_first_client"));
	}

}
