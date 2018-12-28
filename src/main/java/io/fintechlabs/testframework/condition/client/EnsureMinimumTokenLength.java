package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class EnsureMinimumTokenLength extends AbstractCondition {

	private final double requiredLength = 128;

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public EnsureMinimumTokenLength(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("token_endpoint_response", "access_token");

		if (Strings.isNullOrEmpty(accessToken)) {
			throw error("Can't find access token");
		}

		byte[] bytes = accessToken.getBytes();

		int bitLength = bytes.length * 8;

		if (bitLength >= requiredLength) {
			logSuccess("Access token is of sufficient length", args("required", requiredLength, "actual", bitLength));
			return env;
		} else {
			throw error("Access token is not of sufficient length", args("required", requiredLength, "actual", bitLength));
		}

	}

}
