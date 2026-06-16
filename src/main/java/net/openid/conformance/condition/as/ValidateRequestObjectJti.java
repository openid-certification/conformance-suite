package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class ValidateRequestObjectJti extends AbstractCondition {

	private static final String NAMESPACE = "request_object_jti";

	@Override
	@PreEnvironment(required = {"authorization_request_object"})
	public Environment evaluate(Environment env) {
		JsonElement jtiElement = env.getElementFromObject("authorization_request_object", "claims.jti");

		if (jtiElement == null) {
			throw error("Missing jti, request object does not contain a 'jti' claim");
		}

		if (!jtiElement.isJsonPrimitive() || !jtiElement.getAsJsonPrimitive().isString()) {
			throw error("'jti' claim in request object is not a string", args("jti", jtiElement));
		}

		String jti = OIDFJSON.getString(jtiElement);
		if (jti.isEmpty()) {
			throw error("'jti' claim in request object cannot be an empty string", args("jti", jti));
		}

		// Reuse is detected across requests/runs via a process-wide store (each module finishes after one
		// request, so a per-module Environment can never see two request objects). Scoped to the logged-in
		// suite user (owner_id, set unconditionally by the test module's configure() via
		// exposeOwnerIdToEnvironment()), so its absence is a test suite bug.
		String scope = env.getString("owner_id");
		if (scope == null || scope.isBlank()) {
			throw error("owner_id is missing from the environment - this is a test suite bug; the test "
				+ "module must surface it via exposeOwnerIdToEnvironment() in configure()");
		}

		RecentValueHistory.SeenValue reused = RecentValueHistory.checkAndRecord(NAMESPACE, scope, List.of(jti), getTestId());
		if (reused != null) {
			throw error("'jti' claim in request object has already been used in a previous request",
				args("jti", jti, "first_seen_in_test", reused.testId()));
		}

		logSuccess("'jti' claim is a non-empty string and has not been used in a previous request",
			args("jti", jtiElement));
		return env;
	}

}
