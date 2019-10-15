package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidOpenBankingIntentIdToIdToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token_claims", strings = "openbanking_intent_id")
	@PostEnvironment(required = "id_token_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("id_token_claims");

		String intent = env.getString("openbanking_intent_id");

		//Add number 1 onto end of intent string
		String concat = intent + 1;

		claims.addProperty("openbanking_intent_id", concat);

		env.putObject("id_token_claims", claims);

		logSuccess("Added invalid openbanking_intent_id to ID token claims", args("id_token_claims", claims, "openbanking_intent_id", concat));

		return env;

	}

}
