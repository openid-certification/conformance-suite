package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CreateTokenEndpointRequestForAuthorizationCodeGrant extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CreateTokenEndpointRequestForAuthorizationCodeGrant(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = { "code", "redirect_uri" })
	@PostEnvironment(required = "token_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = new JsonObject();
		o.addProperty("grant_type", "authorization_code");
		o.addProperty("code", env.getString("code"));
		o.addProperty("redirect_uri", env.getString("redirect_uri"));

		env.putObject("token_endpoint_request_form_parameters", o);

		logSuccess(o);

		return env;
	}

}
