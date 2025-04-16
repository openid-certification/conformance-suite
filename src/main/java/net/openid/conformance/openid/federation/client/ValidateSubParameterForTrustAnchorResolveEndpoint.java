package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class ValidateSubParameterForTrustAnchorResolveEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"resolve_endpoint_parameters", "config"})
	@PostEnvironment(strings = "resolve_endpoint_parameter_sub")
	public Environment evaluate(Environment env) {

		String sub = env.getString("resolve_endpoint_parameters", "sub");

		if (sub == null || sub.isEmpty()) {
			env.putString("federation_resolve_endpoint_error", "invalid_request");
			env.putString("federation_resolve_endpoint_error_description", "Missing required parameter sub in request");
			env.putInteger("federation_resolve_endpoint_status_code", 400);
			throw error("Missing required sub parameter in request");
		}

		JsonArray immediateSubordinates = env.getElementFromObject("config", "federation_trust_anchor.immediate_subordinates").getAsJsonArray();
		List<String> immediateSubordinatesList = OIDFJSON.convertJsonArrayToList(immediateSubordinates);

		if (!immediateSubordinatesList.contains(sub)) {
			env.putString("federation_resolve_endpoint_error", "invalid_subject");
			env.putString("federation_resolve_endpoint_error_description",
				"Parameter sub %s not found in the test configuration (Federation trust anchor -> immediate_subordinates)".formatted(sub));
			env.putInteger("federation_resolve_endpoint_status_code", 404);
			throw error("Parameter sub not found in immediate subordinates",
				args("sub", sub, "immediate_subordinates", immediateSubordinates));
		}

		env.putString("resolve_endpoint_parameter_sub", sub);
		logSuccess("sub %s is an immediate subordinate of the trust anchor".formatted(sub), args("sub", sub));

		return env;
	}
}
