package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class ValidateSubParameterForTrustAnchorFetchEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request", strings = "trust_anchor_entity_identifier")
	@PostEnvironment(strings = "fetch_endpoint_parameter_sub")
	public Environment evaluate(Environment env) {

		String sub;
		if (env.getString("incoming_request", "method").equalsIgnoreCase("POST")) {
			sub = env.getString("incoming_request", "body_form_params.sub");
		} else {
			sub = env.getString("incoming_request", "query_string_params.sub");
		}

		if (sub == null || sub.isEmpty()) {
			env.putString("federation_fetch_endpoint_error", "invalid_request");
			env.putString("federation_fetch_endpoint_error_description", "Missing required sub parameter in request");
			env.putInteger("federation_fetch_endpoint_status_code", 400);
			throw error("Missing required sub parameter in request");
		}

		String trustAnchorEntityIdentifier = env.getString("trust_anchor_entity_identifier");
		if (sub.equalsIgnoreCase(trustAnchorEntityIdentifier)) {
			env.putString("federation_fetch_endpoint_error", "invalid_request");
			env.putString("federation_fetch_endpoint_error_description", "The sub parameter references the Entity Identifier of the Issuing Entity");
			env.putInteger("federation_fetch_endpoint_status_code", 400);
			throw error("The sub parameter references the Entity Identifier of the Issuing Entity", args("sub", sub, "entity_identifier", trustAnchorEntityIdentifier));
		}

		JsonArray immediateSubordinates;
		JsonElement immediateSubordinatesElement = env.getElementFromObject("config", "federation_trust_anchor.immediate_subordinates");
		if (immediateSubordinatesElement != null) {
			immediateSubordinates = immediateSubordinatesElement.getAsJsonArray();
			List<String> immediateSubordinatesList = OIDFJSON.convertJsonArrayToList(immediateSubordinates);
			if(!immediateSubordinatesList.contains(sub)) {
				env.putString("federation_fetch_endpoint_error", "not_found");
				env.putString("federation_fetch_endpoint_error_description", "%s is not configured as an immediate subordinate".formatted(sub));
				env.putInteger("federation_fetch_endpoint_status_code", 404);
				throw error("%s is not configured as an immediate subordinate".formatted(sub),
					args("sub", sub, "immediate_subordinates", immediateSubordinates));
			}
		}

		env.putString("fetch_endpoint_parameter_sub", sub);
		logSuccess("sub %s is an immediate subordinate".formatted(sub), args("sub", sub));

		return env;
	}
}
