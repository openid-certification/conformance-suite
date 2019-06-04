package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class FAPIRWCheckTLSClientCertificateBoundAccessTokens extends AbstractCondition {

	public FAPIRWCheckTLSClientCertificateBoundAccessTokens(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement element = env.getElementFromObject("server", "tls_client_certificate_bound_access_tokens");
		if (element == null || element.isJsonObject()) {
			throw error("tls_client_certificate_bound_access_tokens in server was missing");
		}

		if (!element.getAsJsonPrimitive().isBoolean()) {
			throw error("Type of tls_client_certificate_bound_access_tokens must be boolean.");
		}

		if (!OIDFJSON.getBoolean(element)) {
			throw error("tls_client_certificate_bound_access_tokens must be 'true'", args("actual", OIDFJSON.getBoolean(element)));
		}

		logSuccess("tls_client_certificate_bound_access_tokens was 'true'");

		return env;
	}
}
