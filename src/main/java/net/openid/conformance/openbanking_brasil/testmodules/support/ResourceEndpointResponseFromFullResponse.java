package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ResourceEndpointResponseFromFullResponse extends AbstractCondition {
	/**
	 * A condition that has the purpose of assuring retro-compatibility between the most recent resource_endpoint_response_full
	 * object and the string resource_endpoint_response. This condition is specially handy when dealing with Consents API
	 * calls where status code and body validations have to be made.
	 */

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	@PostEnvironment(strings = { "resource_endpoint_response" })
	public Environment evaluate(Environment env) {

		String body = env.getString("resource_endpoint_response_full", "body");

		if(Strings.isNullOrEmpty(body)) {
			throw error("Empty body from endpoint", args("body", body));
		}
		env.putString("resource_endpoint_response", body);

		logSuccess("New resource_endpoint_response string created");
		return env;
	}
}
