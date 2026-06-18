package net.openid.conformance.openid.ssf.conditions.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfConstants;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Validates the {@code scope} form parameter on a client_credentials token
 * request against the reserved SSF scopes (CAEP Interop Profile §2.7.3
 * "OAuth Scopes"): {@code ssf.read} and {@code ssf.manage}. The validated scope string is
 * echoed into {@code env.scope} so {@link
 * net.openid.conformance.condition.as.CreateTokenEndpointResponse} includes it
 * in the token endpoint response.
 */
public class OIDSSFValidateRequestedScope extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	@PostEnvironment(strings = "scope")
	public Environment evaluate(Environment env) {

		String scopeParam = env.getString("token_endpoint_request", "body_form_params.scope");

		if (scopeParam == null || scopeParam.isBlank()) {
			throw error("Token request is missing the required 'scope' parameter. "
				+ "Receivers must request at least one of the reserved SSF scopes.",
				args("allowed_scopes", SsfConstants.SSF_SCOPES));
		}

		Set<String> requestedScopes = new LinkedHashSet<>(Arrays.asList(scopeParam.trim().split("\\s+")));

		Set<String> unknownScopes = new LinkedHashSet<>(requestedScopes);
		unknownScopes.removeAll(SsfConstants.SSF_SCOPES);

		if (!unknownScopes.isEmpty()) {
			throw error("Token request contains scopes that are not valid SSF scopes",
				args("requested_scope", scopeParam, "unknown_scopes", unknownScopes,
					"allowed_scopes", SsfConstants.SSF_SCOPES));
		}

		env.putString("scope", scopeParam);

		logSuccess("Validated requested SSF scope", args("scope", scopeParam));

		return env;
	}
}
