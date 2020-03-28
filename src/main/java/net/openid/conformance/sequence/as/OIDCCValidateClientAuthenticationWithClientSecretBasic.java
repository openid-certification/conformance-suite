package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.EnsureClientAssertionTypeIsJwt;
import net.openid.conformance.condition.as.ExtractClientAssertion;
import net.openid.conformance.condition.as.ExtractClientCredentialsFromBasicAuthorizationHeader;
import net.openid.conformance.condition.as.ValidateClientAssertionClaims;
import net.openid.conformance.condition.as.ValidateClientIdAndSecret;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCValidateClientAuthenticationWithClientSecretBasic extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		callAndStopOnFailure(ExtractClientCredentialsFromBasicAuthorizationHeader.class);

		callAndStopOnFailure(ValidateClientIdAndSecret.class, "RFC6749-2.3.1");
	}
}
