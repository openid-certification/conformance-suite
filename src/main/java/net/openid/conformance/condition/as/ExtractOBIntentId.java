package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractOBIntentId extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object")
	@PostEnvironment(strings = "openbanking_intent_id")
	public Environment evaluate(Environment env) {

		String intentId = env.getString("authorization_request_object", "claims.claims.id_token.openbanking_intent_id.value");
		Boolean essential = env.getBoolean("authorization_request_object", "claims.claims.id_token.openbanking_intent_id.essential");

		if (essential != null && essential.booleanValue()) {

			logSuccess("Found open banking intent ID", args("openbanking_intent_id", intentId));

			env.putString("openbanking_intent_id", intentId);

			return env;

		} else {
			throw error("Missing required 'essential' claim in request object");
		}

	}

}
