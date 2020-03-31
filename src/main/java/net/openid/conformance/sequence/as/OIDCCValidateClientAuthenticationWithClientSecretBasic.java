package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.EnsureClientAssertionTypeIsJwt;
import net.openid.conformance.condition.as.ExtractClientAssertion;
import net.openid.conformance.condition.as.ExtractClientCredentialsFromBasicAuthorizationHeader;
import net.openid.conformance.condition.as.ValidateClientAssertionClaims;
import net.openid.conformance.condition.as.ValidateClientIdAndSecret;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCValidateClientAuthenticationWithClientSecretBasic extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		callAndContinueOnFailure(ExtractClientCredentialsFromBasicAuthorizationHeader.class, Condition.ConditionResult.FAILURE, "OIDCC-9");

		callAndContinueOnFailure(ValidateClientIdAndSecret.class, Condition.ConditionResult.FAILURE, "RFC6749-2.3.1");
	}
}
