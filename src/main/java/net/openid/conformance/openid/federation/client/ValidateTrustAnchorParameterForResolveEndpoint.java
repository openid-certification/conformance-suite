package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateTrustAnchorParameterForResolveEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "resolve_endpoint_parameters" }, strings = "trust_anchor_entity_identifier")
	@PostEnvironment(strings = "resolve_endpoint_parameter_trust_anchor")
	public Environment evaluate(Environment env) {
		String trustAnchor = env.getString("resolve_endpoint_parameters", "trust_anchor");
		String expectedTrustAnchor = env.getString("trust_anchor_entity_identifier");

		if (trustAnchor == null) {
			env.putString("federation_resolve_endpoint_error", "invalid_request");
			env.putString("federation_resolve_endpoint_error_description", "Missing required parameter trust_anchor in request");
			env.putInteger("federation_resolve_endpoint_status_code", 400);
			throw error("Missing trust_anchor parameter");
		}

		if (!trustAnchor.equals(expectedTrustAnchor)) {
			env.putString("federation_resolve_endpoint_error", "invalid_trust_anchor");
			env.putString("federation_resolve_endpoint_error_description", "The trust_anchor cannot be found or used");
			env.putInteger("federation_resolve_endpoint_status_code", 404);
			throw error("Invalid trust_anchor parameter", args("expected", expectedTrustAnchor, "actual", trustAnchor));
		}

		env.putString("resolve_endpoint_parameter_trust_anchor", trustAnchor);

		logSuccess("Trust anchor parameter is valid", args("trust_anchor", trustAnchor));
		return env;
	}
}
