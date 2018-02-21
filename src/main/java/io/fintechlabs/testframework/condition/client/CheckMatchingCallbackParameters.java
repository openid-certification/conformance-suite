package io.fintechlabs.testframework.condition.client;

import java.util.Map;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckMatchingCallbackParameters extends AbstractCondition {

	public CheckMatchingCallbackParameters(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = "redirect_uri", required = "callback_query_params")
	public Environment evaluate(Environment env) {

		UriComponents redirectUri = UriComponentsBuilder.fromHttpUrl(env.getString("redirect_uri")).build();

		Map<String, String> params = redirectUri.getQueryParams().toSingleValueMap();

		JsonObject o = new JsonObject(); // For the log

		for (Map.Entry<String, String> entry : params.entrySet()) {
			String key = entry.getKey();
			String expected = entry.getValue();
			String actual = env.getString("callback_query_params", key);

			if (!expected.equals(actual)) {
				return error("Callback parameter invalid or missing", args("parameter", key, "expected", expected, "actual", actual));
			}

			o.addProperty(key, expected);
		}

		logSuccess("Callback parameters successfully verified", o);

		return env;
	}

}
