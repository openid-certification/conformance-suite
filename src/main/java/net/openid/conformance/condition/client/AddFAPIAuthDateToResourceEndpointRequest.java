package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.hc.client5.http.utils.DateUtils;

import java.util.Date;

public class AddFAPIAuthDateToResourceEndpointRequest extends AbstractCondition {

	@Override
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {

		Date loginDate = new Date(); // User just logged in

		JsonObject headers = env.getObject("resource_endpoint_request_headers");

		headers.addProperty("x-fapi-auth-date", DateUtils.formatStandardDate(DateUtils.toInstant(loginDate)));

		logSuccess("Added x-fapi-auth-date to resource endpoint request headers", args("resource_endpoint_request_headers", headers));

		return env;
	}

}
