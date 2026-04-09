package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BuildVP1FinalBrowserDCAPIRequestMultiSigned extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_json")
	@PostEnvironment(required = "browser_api_request")
	public Environment evaluate(Environment env) {
		JsonObject requestObjectJson = env.getObject("request_object_json");

		var data = new JsonObject();
		// For multi-signed, data.request is a JSON object (JWS JSON Serialization), not a string
		data.add("request", requestObjectJson);

		var request = new JsonObject();
		request.addProperty("protocol", "openid4vp-v1-multisigned");
		request.add("data", data);

		var requestsArray = new JsonArray();
		requestsArray.add(request);

		var digital = new JsonObject();
		digital.add("requests", requestsArray);

		var obj = new JsonObject();
		obj.add("digital", digital);

		env.putObject("browser_api_request", obj);
		log("Created Browser API request for multi-signed", args("object", obj));

		return env;
	}

}
