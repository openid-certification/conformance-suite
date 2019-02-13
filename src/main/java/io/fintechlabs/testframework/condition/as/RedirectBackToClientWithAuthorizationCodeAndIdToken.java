package io.fintechlabs.testframework.condition.as;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class RedirectBackToClientWithAuthorizationCodeAndIdToken extends AbstractCondition {

	public RedirectBackToClientWithAuthorizationCodeAndIdToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "authorization_endpoint_request", strings = {"authorization_code", "id_token"})
	@PostEnvironment(strings = "authorization_endpoint_response_redirect")
	public Environment evaluate(Environment env) {

		String idToken = env.getString("id_token");
		String code = env.getString("authorization_code");
		String state = env.getString("authorization_endpoint_request", "params.state");
		String redirectUri = env.getString("authorization_endpoint_request", "params.redirect_uri");


		String params = UriComponentsBuilder.newInstance()
			.queryParam("state", state)
			.queryParam("code", code)
			.queryParam("id_token", idToken)
			.toUriString();

		if(params.startsWith("?")) {
			params = params.substring(1);
		}

		String redirectTo = redirectUri + "#" + params;

		logSuccess("Redirecting back to client", args("uri", redirectTo));

		env.putString("authorization_endpoint_response_redirect", redirectTo);

		return env;

	}

}
