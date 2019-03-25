package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckTokenEndpointCacheHeaders extends AbstractCondition {

	public CheckTokenEndpointCacheHeaders(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment( required = "token_endpoint_response_headers" )
	public Environment evaluate(Environment env) {
		JsonObject headers = env.getObject("token_endpoint_response_headers");

		if (headers == null) {
			throw error("Headers token endpoint can not be null");
		}

		if (headers.has("cache-control")) {

			String expected = "no-store";
			String cacheControl = headers.get("cache-control").getAsString();

			if (Strings.isNullOrEmpty(cacheControl) || !isStringsContainElement(cacheControl.split(","), expected)) {
				throw error("'cache-control' in the headers doesn't contain expected value.", args("expected", expected, "actual", cacheControl));
			}
		}

		if (headers.has("pragma")) {

			String expected = "no-cache";
			String pragma = headers.get("pragma").getAsString();

			if (Strings.isNullOrEmpty(pragma) || !isStringsContainElement(pragma.split(","), expected)) {
				throw error("'pragma' in the headers doesn't contain expected value.", args("expected", expected, "actual", pragma));
			}

		}

		logSuccess("Checked 'pragma' and 'cache-control' in the headers of token_endpoint_response.");

		return env;
	}

	private boolean isStringsContainElement(String[] strings, String expected) {
		if (strings == null || strings.length == 0) {
			return false;
		}

		for (String string : strings) {
			if (string.trim().equals(expected)) {
				return true;
			}
		}

		return false;
	}
}
