package io.fintechlabs.testframework.condition.as;

import java.util.UUID;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreateFapiInteractionIdIfNeeded extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public CreateFapiInteractionIdIfNeeded(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PostEnvironment(strings = "fapi_interaction_id")
	public Environment evaluate(Environment env) {
		String fapiInteractionId = env.getString("fapi_interaction_id");

		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			fapiInteractionId = UUID.randomUUID().toString();

			logSuccess("Created new FAPI interaction ID", args("fapi_interaction_id", fapiInteractionId));

			env.putString("fapi_interaction_id", fapiInteractionId);

		} else {
			// if there's an existing one we just leave it there
			log("Found existing FAPI interaction ID",
				args("fapi_interaction_id", fapiInteractionId, "result", ConditionResult.INFO));
		}

		return env;

	}

}
