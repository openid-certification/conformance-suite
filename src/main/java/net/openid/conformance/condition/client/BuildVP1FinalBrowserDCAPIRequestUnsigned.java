package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BuildVP1FinalBrowserDCAPIRequestUnsigned extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request" })
	@PostEnvironment(required = "browser_api_request")
	public Environment evaluate(Environment env) {
		var data = env.getObject("authorization_endpoint_request");

		var request = new JsonObject();
		request.addProperty("protocol", "openid4vp-v1-unsigned");
		request.add("data", data);

		var requestsArray = new JsonArray();
		requestsArray.add(request);

		var digital = new JsonObject();
		digital.add("requests", requestsArray);

		var obj = new JsonObject();
		obj.add("digital", digital);

		env.putObject("browser_api_request", obj);
		log("Created Browser API request", args("object", obj));

		return env;
	}

}
