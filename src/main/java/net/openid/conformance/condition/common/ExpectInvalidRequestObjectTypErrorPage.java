package net.openid.conformance.condition.common;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectInvalidRequestObjectTypErrorPage extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "invalid_request_object_typ_error")
	public Environment evaluate(Environment env) {

		String placeholder = createBrowserInteractionPlaceholder(
			"The request object's 'typ' header is missing or is not 'oauth-authz-req+jwt'. "
				+ "The wallet must not process the request object and should display an error.");
		env.putString("invalid_request_object_typ_error", placeholder);

		return env;
	}
}
