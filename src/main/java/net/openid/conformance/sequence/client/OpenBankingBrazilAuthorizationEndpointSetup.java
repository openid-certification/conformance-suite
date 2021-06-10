package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddConsentScopeToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OpenBankingBrazilAuthorizationEndpointSetup extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddConsentScopeToAuthorizationEndpointRequest.class);
	}

}
