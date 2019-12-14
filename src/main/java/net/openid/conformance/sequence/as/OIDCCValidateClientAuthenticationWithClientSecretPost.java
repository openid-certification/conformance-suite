package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.ExtractClientCredentialsFromBasicAuthorizationHeader;
import net.openid.conformance.condition.as.ExtractClientCredentialsFromFormPost;
import net.openid.conformance.condition.as.ValidateClientIdAndSecret;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCValidateClientAuthenticationWithClientSecretPost extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		//TODO ensure the client secret was sent in the post body not query string. Depends on #666
		callAndStopOnFailure(ExtractClientCredentialsFromFormPost.class);

		callAndStopOnFailure(ValidateClientIdAndSecret.class);
	}
}
