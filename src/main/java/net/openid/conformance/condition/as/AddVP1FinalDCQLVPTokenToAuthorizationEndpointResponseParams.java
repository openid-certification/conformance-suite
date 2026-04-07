package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddVP1FinalDCQLVPTokenToAuthorizationEndpointResponseParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CreateAuthorizationEndpointResponseParams.ENV_KEY, ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY}, strings = "credential")
	@PostEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonObject params = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY);

		JsonObject dcql = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY);
		JsonElement credentials = dcql.get("credentials");
		if (credentials == null || !credentials.isJsonArray()) {
			throw error("'credentials' within dcql object is missing or not an array",
				args("dcql_query", dcql));
		}
		// Use the first credential entry — in tests with credential_sets, the first
		// entry is always the required/matching credential.
		JsonElement credentialRequest = credentials.getAsJsonArray().get(0);
		if (!credentialRequest.isJsonObject()) {
			throw error("First entry of 'credentials' array within dcql object is not a JSON object",
				args("dcql_query", dcql));
		}
		JsonElement idEl = credentialRequest.getAsJsonObject().get("id");
		if (idEl == null) {
			throw error("'id' within first entry of 'credentials' array within dcql object is missing",
				args("dcql_query", dcql));
		}
		if (!idEl.isJsonPrimitive() || !idEl.getAsJsonPrimitive().isString()) {
			throw error("'id' within first entry of 'credentials' array within dcql object is not a string",
				args("dcql_query", dcql));
		}
		String id = OIDFJSON.getString(idEl);

		String credential = env.getString("credential");

		JsonArray credentialArray = new JsonArray();
		credentialArray.add(credential);

		JsonObject vpToken = new JsonObject();
		vpToken.add(id, credentialArray);

		params.add("vp_token", vpToken);

		logSuccess("Added credential in DCQL 'vp_token' authorization endpoint response parameter", args(CreateAuthorizationEndpointResponseParams.ENV_KEY, params));

		return env;

	}

}
