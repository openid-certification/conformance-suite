package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BuildVCIDCAPIRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"vci", "server", "credential_issuer_metadata"})
	@PostEnvironment(required = "browser_api_request")
	public Environment evaluate(Environment env) {
		/*
		as per example in https://github.com/openid/OpenID4VCI/pull/476/files#diff-1f424614b35a9899813079f1b1f6218631a2aedd993368ccb89bb81a9eda0289R2102
   digital: {
	 requests: [
	   {
		 protocol: "openid4vci-v1",
		 data: {
		   credential_issuer: "...",
		   credential_configuration_ids: [...],
		   grants: {...}
		   credential_issuer_metadata: {...},
		   authorization_server_metadata: {...}
		 }
	   }
	 ]
   }
		 */
		var data = env.getElementFromObject("vci", "credential_offer").getAsJsonObject();

		data.add("credential_issuer_metadata", env.getObject("credential_issuer_metadata"));
		data.add("authorization_server_metadata", env.getObject("server"));

		var request = new JsonObject();
		request.addProperty("protocol", "openid4vci-v1");
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
