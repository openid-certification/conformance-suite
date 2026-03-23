package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckForUnexpectedClaimsInIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_unknown_claims")
	public Environment evaluate(Environment env) {

		// This is populated by ValidateIdTokenStandardClaims
		JsonObject unknownClaims = env.getObject("id_token_unknown_claims");

		// If this check was to be used more generally, it'd make sense for it to remove any claims we'd explicitly
		// requested, e.g. openbanking_intent_id in the OpenBanking UK tests

		if (unknownClaims.size() != 0) {
			throw error("id_token includes claims with names that are not known. This may indicate the authorization server has misunderstood the spec, or it may be using extensions the test suite is unaware of.", unknownClaims);
		}

		logSuccess("id_token includes only known claims");

		return env;
	}

}
