package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddFAPIFinancialIdToResourceEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		// get the configured financial ID
		String financialId = env.getString("client", "fapi_financial_id");
		if (Strings.isNullOrEmpty(financialId)) {
			throw error("Couldn't find financial ID");
		}

		// get the previous headers if they exist
		JsonObject headers = env.getObject("resource_endpoint_request_headers");
		if (headers == null) {
			headers = new JsonObject();
		}

		headers.addProperty("x-fapi-financial-id", financialId);

		env.putObject("resource_endpoint_request_headers", headers);

		return env;

	}

}
