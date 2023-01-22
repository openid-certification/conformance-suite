package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.rs.CreateBrazilResourcesEndpointResponse;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class GenerateOpenInsuranceBrazilResourcesEndpointResponse extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateBrazilResourcesEndpointResponse.class);
	}

}
