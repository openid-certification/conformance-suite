package net.openid.conformance.poppy.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.poppy.PoppyConstants;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class EnsurePoppyAuthorizationRequestContainsScope extends AbstractCondition {

	private static final String REQUEST = "effective_authorization_endpoint_request";

	@Override
	@PreEnvironment(required = REQUEST)
	public Environment evaluate(Environment env) {
		String scope = env.getString(REQUEST, "scope");
		if (scope == null || scope.isBlank()) {
			throw error("The authorization request has no 'scope'. Section 4.7 requires every sign-in request to include 'scope'.");
		}

		List<String> unknown = new ArrayList<>();
		for (String s : scope.trim().split(" +")) {
			if (!PoppyConstants.COMPANY_SCOPES.contains(s)) {
				unknown.add(s);
			}
		}
		if (!unknown.isEmpty()) {
			throw error("The authorization request asks for scopes this Company does not define, so the Company answers with invalid_scope. The scopes the Company publishes are " + PoppyConstants.COMPANY_SCOPES + ".",
				args("scope", scope, "unknown_scopes", unknown, "company_scopes", PoppyConstants.COMPANY_SCOPES));
		}
		logSuccess("The authorization request contains a scope the Company defines", args("scope", scope));
		return env;
	}
}
