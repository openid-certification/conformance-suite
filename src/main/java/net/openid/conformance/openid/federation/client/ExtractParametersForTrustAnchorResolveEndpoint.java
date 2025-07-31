package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractParametersForTrustAnchorResolveEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request", strings = "trust_anchor_entity_identifier")
	@PostEnvironment(required = "resolve_endpoint_parameters")
	@SuppressWarnings("unused")
	public Environment evaluate(Environment env) {

		String sub = env.getString("incoming_request", "query_string_params.sub");
		String trustAnchor = env.getString("incoming_request", "query_string_params.trust_anchor");

		JsonElement entityTypeElement = env.getElementFromObject("incoming_request", "query_string_params.entity_type");
		JsonArray entityTypes;
		if (entityTypeElement != null) {
			if (entityTypeElement.isJsonArray()) {
				entityTypes = entityTypeElement.getAsJsonArray();
			} else {
				entityTypes = new JsonArray();
				entityTypes.add(OIDFJSON.getString(entityTypeElement));
			}
		} else {
			entityTypes = new JsonArray();
		}

		JsonObject resolveEndpointParameters = new JsonObject();
		resolveEndpointParameters.addProperty("sub", sub);
		resolveEndpointParameters.addProperty("trust_anchor", trustAnchor);
		resolveEndpointParameters.add("entity_types", entityTypes);
		env.putObject("resolve_endpoint_parameters", resolveEndpointParameters);

		return env;
	}
}
