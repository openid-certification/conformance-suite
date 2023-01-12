package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public abstract class AbstractCheckErrorFromDynamicRegistrationEndpoint extends AbstractCondition {
	abstract List<String> getPermittedErrors();

	@Override
	@PreEnvironment(required = "dynamic_registration_endpoint_response")
	public Environment evaluate(Environment env) {

		String error = env.getString("dynamic_registration_endpoint_response", "body_json.error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("'error' field not found in response from dynamic registration endpoint");
		}

		List<String> permittedErrors = getPermittedErrors();

		if (!permittedErrors.contains(error)) {
			throw error("'error' field has unexpected value", args("permitted", permittedErrors, "actual", error));
		}

		logSuccess("Dynamic registration endpoint returned 'error'", args("permitted", permittedErrors, "error", error));

		return env;
	}
}
