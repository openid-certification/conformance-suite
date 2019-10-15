package net.openid.conformance.condition.client;

import java.util.Date;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.http.client.utils.DateUtils;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;

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
