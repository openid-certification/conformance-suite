package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Adds a redirect_uri parameter alongside the response_uri in the authorization request.
 * For direct_post mode, only response_uri should be present — adding redirect_uri
 * tests that the wallet rejects this invalid combination.
 */
public class AddRedirectUriAlongsideResponseUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	public Environment evaluate(Environment env) {

		JsonObject request = env.getObject("authorization_endpoint_request");
		String responseUri = request.has("response_uri") ? OIDFJSON.getString(request.get("response_uri")) : env.getString("base_url");

		request.addProperty("redirect_uri", responseUri);

		log("Added redirect_uri alongside response_uri in authorization request",
			args("redirect_uri", responseUri));

		return env;
	}
}
