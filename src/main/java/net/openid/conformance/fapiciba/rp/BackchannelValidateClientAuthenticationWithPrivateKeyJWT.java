package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.*;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class BackchannelValidateClientAuthenticationWithPrivateKeyJWT extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(BackchannelExtractClientAssertion.class, Condition.ConditionResult.FAILURE, "RFC7523-2.2");
		callAndContinueOnFailure(BackchannelEnsureClientAssertionSignatureAlgorithmMatchesRegistered.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		callAndContinueOnFailure(ValidateClientAssertionSignature.class, Condition.ConditionResult.FAILURE, "OIDCC-9");
		callAndContinueOnFailure(BackchannelEnsureClientAssertionTypeIsJwt.class, Condition.ConditionResult.FAILURE, "RFC7523-2.2");
		callAndContinueOnFailure(BackchannelValidateClientAssertionClaims.class, Condition.ConditionResult.FAILURE, "RFC7523-3", "OIDCC-9");
	}
}
