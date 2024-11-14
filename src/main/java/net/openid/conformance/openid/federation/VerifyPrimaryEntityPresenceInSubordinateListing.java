package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Objects;

public class VerifyPrimaryEntityPresenceInSubordinateListing extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "federation_endpoint_response" } )
	public Environment evaluate(Environment env) {

		JsonObject listEndpointResponse = env.getObject("federation_endpoint_response");
		JsonElement listEndpointResponseBody = JsonParser.parseString(OIDFJSON.getString(listEndpointResponse.get("body")));

		if (listEndpointResponseBody == null || !listEndpointResponseBody.isJsonArray()) {
			throw error("Subordinate listing response must be a JSON Array", args("federation_endpoint_response", listEndpointResponse));
		}

		String primaryEntitySub = env.getString("primary_entity_statement_sub");
		JsonArray subordinateListing = listEndpointResponseBody.getAsJsonArray();
		for (JsonElement element : subordinateListing) {
			if (Objects.equals(primaryEntitySub, OIDFJSON.getString(element))) {
				logSuccess("Found primary entity in subordinate listing response", args("federation_endpoint_response", listEndpointResponseBody));
				return env;
			}
		}

		throw error("Subordinate listing response did not contain the primary entity", args("federation_endpoint_response", listEndpointResponseBody));
	}
}
