package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CdrEnsureIntrospectionResponseArrangementIdMatchesTokenResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CallTokenIntrospectionEndpoint.RESPONSE_KEY, "token_endpoint_response" })
	public Environment evaluate(Environment env) {

		String introspectionArrangementId = env.getString(CallTokenIntrospectionEndpoint.RESPONSE_KEY, "body_json.cdr_arrangement_id");

		if (Strings.isNullOrEmpty(introspectionArrangementId)) {
			throw error("cdr_arrangement_id is missing from the introspection response; CDR requires it for active tokens",
				args("introspection_response", env.getElementFromObject(CallTokenIntrospectionEndpoint.RESPONSE_KEY, "body_json")));
		}

		String tokenResponseArrangementId = env.getString("token_endpoint_response", "cdr_arrangement_id");

		if (Strings.isNullOrEmpty(tokenResponseArrangementId)) {
			logSuccess("The introspection response contains a cdr_arrangement_id; the token response did not contain one to compare it against",
				args("cdr_arrangement_id", introspectionArrangementId));
			return env;
		}

		if (!introspectionArrangementId.equals(tokenResponseArrangementId)) {
			throw error("The cdr_arrangement_id in the introspection response does not match the one in the token response",
				args("introspection_cdr_arrangement_id", introspectionArrangementId,
					"token_response_cdr_arrangement_id", tokenResponseArrangementId));
		}

		logSuccess("The introspection response contains the same cdr_arrangement_id as the token response",
			args("cdr_arrangement_id", introspectionArrangementId));
		return env;
	}

}
