package io.fintechlabs.testframework.condition.client;

import java.util.Date;

import org.apache.http.client.utils.DateUtils;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class FAPIGenerateResourceEndpointRequestHeaders extends AbstractCondition {

	@Override
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		Date loginDate = new Date(); // User just logged in

		JsonObject headers = new JsonObject();

		headers.addProperty("x-fapi-auth-date", DateUtils.formatDate(loginDate));

		env.putObject("resource_endpoint_request_headers", headers);

		logSuccess("Generated headers", args("resource_endpoint_request_headers", headers));

		return env;
	}

}
