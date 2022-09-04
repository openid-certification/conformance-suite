package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.ExtractClientCredentialsFromFormPost;
import net.openid.conformance.condition.as.ValidateClientIdAndSecret;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCValidateClientAuthenticationWithClientSecretPost extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(ExtractClientCredentialsFromFormPost.class, Condition.ConditionResult.FAILURE, "OIDCC-9");

		callAndContinueOnFailure(ValidateClientIdAndSecret.class, Condition.ConditionResult.FAILURE, "RFC6749-2.3.1");
	}
}
