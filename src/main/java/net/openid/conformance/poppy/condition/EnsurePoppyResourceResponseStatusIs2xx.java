package net.openid.conformance.poppy.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsurePoppyResourceResponseStatusIs2xx extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_full")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("resource_endpoint_response_full");
		JsonElement statusEl = response.get("status");
		if (statusEl == null || !statusEl.isJsonPrimitive()) {
			throw error("The resource endpoint response has no status", args("response", response));
		}
		int status = OIDFJSON.getInt(statusEl);
		if (status < 200 || status >= 300) {
			String wwwAuthenticate = env.getString("resource_endpoint_response_full", "headers.www-authenticate");
			throw error("The Company's protected resource rejected the request carrying a valid access token and Poppy Session JWT with HTTP status " + status + ".",
				args("status", status, "www_authenticate", wwwAuthenticate, "body", response.get("body"),
					"headers", response.get("headers")));
		}
		logSuccess("The protected resource returned a 2xx status", args("status", status));
		return env;
	}
}
