package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddDCQLVPTokenToAuthorizationEndpointResponseParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CreateAuthorizationEndpointResponseParams.ENV_KEY, "authorization_request_object"}, strings = "credential")
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);

		JsonElement dcqlEl = env.getElementFromObject("authorization_request_object", "claims.dcql_query");
		JsonObject authRequestObject = env.getObject("authorization_request_object");
		if (dcqlEl == null) {
			throw error("dcql_query missing from authorization request",
				args("auth_request_object", authRequestObject));
		}
		if (!dcqlEl.isJsonObject()) {
			throw error("dcql_query from authorization request is not a JSON object",
				args("auth_request_object", authRequestObject));
		}
		JsonObject dclEl = dcqlEl.getAsJsonObject();
		JsonElement credentials = dclEl.get("credentials");
		if (credentials == null || !credentials.isJsonArray()) {
			throw error("'credentials' within dcql object is missing or not an array",
				args("auth_request_object", authRequestObject));
		}
		if (credentials.getAsJsonArray().size() != 1) {
			throw error("'credentials' array within dcql object is expected to contain exactly one element for this test",
				args("auth_request_object", authRequestObject));
		}
		JsonElement credentialRequest = credentials.getAsJsonArray().get(0);
		if (!credentialRequest.isJsonObject()) {
			throw error("First entry of 'credentials' array within dcql object is not a JSON object",
				args("auth_request_object", authRequestObject));
		}
		JsonElement idEl = credentialRequest.getAsJsonObject().get("id");
		if (idEl == null) {
			throw error("'id' within first entry of 'credentials' array within dcql object is missing",
				args("auth_request_object", authRequestObject));
		}
		if (!idEl.isJsonPrimitive() || !idEl.getAsJsonPrimitive().isString()) {
			throw error("'id' within first entry of 'credentials' array within dcql object is not a string",
				args("auth_request_object", authRequestObject));
		}
		String id = OIDFJSON.getString(idEl);

		String credential = env.getString("credential");

		JsonObject vpToken = new JsonObject();
		vpToken.addProperty(id, credential);

		params.add("vp_token", vpToken);

		logSuccess("Added credential in DCQL 'vp_token' authorization endpoint response parameter", args(CreateAuthorizationEndpointResponseParams.ENV_KEY, params));

		return env;

	}

}
