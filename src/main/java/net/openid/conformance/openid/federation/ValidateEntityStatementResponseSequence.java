package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureDiscoveryEndpointResponseStatusCodeIs200;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateEntityStatementResponseSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(EnsureDiscoveryEndpointResponseStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		callAndContinueOnFailure(EnsureContentTypeEntityStatementJwt.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
	}
}
