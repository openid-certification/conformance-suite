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
public class AddClientAssertionToRevocationEndpointRequest extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public AddClientAssertionToRevocationEndpointRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "revocation_endpoint_request_form_parameters", strings = "client_assertion")
	@PostEnvironment(required = "revocation_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("revocation_endpoint_request_form_parameters")) {
			throw error("Couldn't find request form");
		}

		JsonObject o = env.getObject("revocation_endpoint_request_form_parameters");

		o.addProperty("client_assertion", env.getString("client_assertion"));
		o.addProperty("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

		env.putObject("revocation_endpoint_request_form_parameters", o);

		log("Added client assertion", o);

		return env;

	}

}
