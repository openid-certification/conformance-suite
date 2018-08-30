package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ExtractFapiInteractionIdHeader extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ExtractFapiInteractionIdHeader(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = "fapi_interaction_id")
	public Environment evaluate(Environment env) {

		String header = env.getString("incoming_request", "headers.x-fapi-interaction-id");
		if (Strings.isNullOrEmpty(header)) {
			throw error("Couldn't find FAPI interaction ID header");
		} else {

			env.putString("fapi_interaction_id", header);
			logSuccess("Found a FAPI interaction ID header", args("fapi_interaction_id", header));

			return env;

		}

	}

}
