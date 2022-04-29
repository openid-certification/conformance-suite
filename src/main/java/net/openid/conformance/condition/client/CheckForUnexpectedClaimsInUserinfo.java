package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckForUnexpectedClaimsInUserinfo extends AbstractCondition {

	@Override
	@PreEnvironment(required = "userinfo_unknown_claims")
	public Environment evaluate(Environment env) {

		// This is populated by ValidateUserinfoStandardClaims
		JsonObject unknownClaims = env.getObject("userinfo_unknown_claims");

		// If this check was to be used more generally, it'd make sense for it to remove any claims we'd explicitly
		// requested, e.g. openbanking_intent_id in the OpenBanking UK tests

		if (unknownClaims.size() != 0) {
			throw error("userinfo response includes claims with names that are not known. This may be because the server supports extensions the test suite is unaware of, or the server may be sending values it should not.", unknownClaims);
		}

		logSuccess("userinfo response includes only known claims");

		return env;
	}

}
