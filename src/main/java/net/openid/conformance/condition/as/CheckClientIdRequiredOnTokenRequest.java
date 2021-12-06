package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckClientIdRequiredOnTokenRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	public Environment evaluate(Environment env) {

		String clientId = env.getString("token_endpoint_request", "body_form_params.client_id");
		String clientAssertion = env.getString("token_endpoint_request", "body_form_params.client_assertion");
		String clientAssertionType = env.getString("token_endpoint_request", "body_form_params.client_assertion_type");

		if (Strings.isNullOrEmpty(clientId)) {
			if (!(Strings.isNullOrEmpty(clientAssertion) ||
				Strings.isNullOrEmpty(clientAssertionType))) {
				logSuccess("Extracted client_assertion and assertion_type found, client_id is not required");
				return env;
			}
			throw error("client_id is required when authenticating clients using mtls");
		}
		logSuccess("Parameter client_id found on the request");
		return env;
	}
}
