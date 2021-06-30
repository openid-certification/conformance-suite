package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddMTLSEndpointAliasesToEnvironment;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class SupportMTLSEndpointAliases extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(AddMTLSEndpointAliasesToEnvironment.class, Condition.ConditionResult.FAILURE, "RFC8705-5");
	}

}
