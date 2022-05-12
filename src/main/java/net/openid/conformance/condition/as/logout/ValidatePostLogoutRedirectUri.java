package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidatePostLogoutRedirectUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"end_session_endpoint_http_request_params", "client"})
	@PostEnvironment()
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
		if(!client.has("post_logout_redirect_uris")) {
			throw error("The client does not have any post_logout_redirect_uris");
		}
		JsonArray registeredUris = client.getAsJsonArray("post_logout_redirect_uris");
		String postLogoutRedirectUri = env.getString("end_session_endpoint_http_request_params", "post_logout_redirect_uri");
		if (postLogoutRedirectUri == null) {
			throw error("no post_logout_redirect_uri passed to end_session_endpoint");
		}
		JsonElement jsonElementForUri = new JsonPrimitive(postLogoutRedirectUri);
		if(!registeredUris.contains(jsonElementForUri)) {
			throw error("Invalid post_logout_redirect_uri in request", args("registered_uris", registeredUris, "actual", jsonElementForUri));
		}
		logSuccess("post_logout_redirect_uri is one of the registered post_logout_redirect_uris",
					args("post_logout_redirect_uri", jsonElementForUri));
		return env;
	}

}
