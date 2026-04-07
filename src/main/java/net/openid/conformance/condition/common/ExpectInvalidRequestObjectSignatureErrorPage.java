package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectInvalidRequestObjectSignatureErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "invalid_request_object_signature_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder(
			"The request object has an invalid signature. "
				+ "The wallet should reject the request and display an error indicating the signature verification failed.");
		env.putString("invalid_request_object_signature_error", placeholder);

		return env;
	}
}
