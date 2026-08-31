package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractAddFAPIEndUserPresentToResourceEndpointRequest extends AbstractCondition {

	protected abstract boolean endUserPresent();

	@Override
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		JsonObject headers = env.getObject("resource_endpoint_request_headers");

		headers.addProperty("x-fapi-end-user-present", Boolean.toString(endUserPresent()));

		logSuccess("Added x-fapi-end-user-present to resource endpoint request headers", args("resource_endpoint_request_headers", headers));

		return env;
	}

}
