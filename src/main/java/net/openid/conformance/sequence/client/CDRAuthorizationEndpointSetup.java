package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.client.AddCdrAcrClaimToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class CDRAuthorizationEndpointSetup extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddCdrAcrClaimToAuthorizationEndpointRequest.class, "CDR-levels-of-assurance-loas");
	}

}
