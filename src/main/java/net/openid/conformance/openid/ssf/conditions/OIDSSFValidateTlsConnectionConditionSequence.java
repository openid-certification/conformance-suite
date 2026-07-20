package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12OrLater;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDSSFValidateTlsConnectionConditionSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(EnsureTLS12OrLater.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.1");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.1");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.1");
	}
}
