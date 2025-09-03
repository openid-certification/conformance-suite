package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFHandleAuthorizationHeader extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject headersParams = env.getElementFromObject("incoming_request", "headers").getAsJsonObject();
		JsonElement authorizationHeaderEl = headersParams.get("authorization");

		JsonObject authResult = new JsonObject();
		env.putObject("ssf", "auth_result", authResult);

		if (authorizationHeaderEl == null) {
			authResult.add("error", createErrorObj("unauthorized", "Missing authorization header in request"));
			authResult.addProperty("status_code", 401);
			log("Missing authorization header in request");
			return env;
		}

		String authorizationHeader = OIDFJSON.getString(authorizationHeaderEl);
		String transmitterToken = env.getString("ssf", "transmitter_access_token");

		String expectedAuthorizationHeader = "Bearer " + transmitterToken;

		if (!expectedAuthorizationHeader.equals(authorizationHeader)) {
			authResult.add("error", createErrorObj("unauthorized", "Invalid authorization header in request"));
			authResult.addProperty("status_code", 401);

			log("Invalid Authorization header present in request", args("authorization_header", authorizationHeader,
				"expected_authorization_header", expectedAuthorizationHeader));
			return env;
		}

		logSuccess("Found valid Authorization header in request",  args("authorization_header", authorizationHeader));

		return env;
	}
}
