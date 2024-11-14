package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class GetStaticEntityStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = { "federation_endpoint_response", } )
	public Environment evaluate(Environment env) {

		JsonElement entityConfiguration = env.getElementFromObject("config", "federation.entity_configuration");
		if (entityConfiguration == null) {
			throw error("Couldn't find entity configuration in test configuration");
		}

		if (!entityConfiguration.isJsonPrimitive() || !entityConfiguration.getAsJsonPrimitive().isString()) {
			throw error("Entity configuration is not a string");
		}

		try {
			String jwtString = OIDFJSON.getString(entityConfiguration);
			JsonObject federationEndpointResponse = new JsonObject();
			federationEndpointResponse.addProperty("body", jwtString);
			env.putObject("federation_endpoint_response", federationEndpointResponse);
			return env;
		} catch (JsonSyntaxException e) {
			throw error(e, args("json", entityConfiguration));
		}
	}

}
