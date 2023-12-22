package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddEssentialAcrClaimWithNoValuesToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class FAPIBrV2AuthorizationEndpointSetup extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddEssentialAcrClaimWithNoValuesToAuthorizationEndpointRequest.class, "");
	}

}
