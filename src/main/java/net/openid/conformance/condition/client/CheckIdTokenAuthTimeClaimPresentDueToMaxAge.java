package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Equivalent of https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-Req-max_age=1.json#L66
 */
public class CheckIdTokenAuthTimeClaimPresentDueToMaxAge extends AbstractCondition {
	private static final String CLAIM_AUTH_TIME = "auth_time";

	@Override
	@PreEnvironment(required = { "id_token" } )
	public Environment evaluate(Environment env) {
		JsonObject idToken = env.getObject("id_token").getAsJsonObject("claims");

		if (!idToken.has(CLAIM_AUTH_TIME)) {
			throw error("auth_time claim is missing from the id_token, but it is required for a authentication where the max_age parameter was used", args("id_token", idToken));
		}

		// no need to check type as ValidateIdToken did so
		logSuccess("auth_time is present in the id_token, as required for a authentication where the max_age parameter was used", args("id_token", idToken));

		return env;
	}

}
