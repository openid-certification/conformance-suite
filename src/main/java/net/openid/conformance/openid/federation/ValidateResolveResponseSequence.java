package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateResolveResponseSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-8.3.2");
		callAndContinueOnFailure(EnsureContentTypeResolveResponseJwt.class, Condition.ConditionResult.FAILURE, "OIDFED-8.3.2");
	}
}
