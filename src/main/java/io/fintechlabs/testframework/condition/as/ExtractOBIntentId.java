package io.fintechlabs.testframework.condition.as;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractOBIntentId extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ExtractOBIntentId(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
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
