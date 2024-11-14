package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateFederationResponseSignatureSequence extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(ValidateServerJWKs.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		callAndContinueOnFailure(VerifyEntityStatmentSignature.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
	}
}
