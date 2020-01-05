package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * this is explicitly checked when plain_http_request is selected
 */
public class EnsureRequestDoesNotContainRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_http_request_params" })
	public Environment evaluate(Environment env) {
		String requestParam = env.getString("authorization_endpoint_http_request_params", "request");

		if (!Strings.isNullOrEmpty(requestParam)) {
			throw error("request parameter is not allowed",
					args("request", requestParam));
		} else {
			logSuccess("Request does not contain a request parameter");
			return env;
		}

	}

}
