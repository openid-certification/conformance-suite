package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * compares the redirect_uri in token request with authorization_endpoint_request_redirect_uri
 */
public class ValidateRedirectUriForTokenEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "token_endpoint_request" }, strings = {"authorization_endpoint_request_redirect_uri"})
	public Environment evaluate(Environment env) {
		String actual = env.getString("token_endpoint_request", "body_form_params.redirect_uri");
		String expected = env.getString("authorization_endpoint_request_redirect_uri");

		if(Strings.isNullOrEmpty(actual)) {
			throw error("redirect_uri is null or empty",
						args("token_endpoint_request", env.getObject("token_endpoint_request")));
		}
		if(actual.equals(expected)) {
			logSuccess("redirect_uri is the same as the one used in the authorization request",
						args("actual", actual));
			return env;
		}
		throw error("redirect_uri is not equal to the one used in the authorization request",
					args("actual", actual, "expected", expected));
	}

}
