package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.OIDFJSON;

public class AddRequestObjectClaimsToBackchannelAuthenticationEndpointRequest extends AbstractCondition {

	public AddRequestObjectClaimsToBackchannelAuthenticationEndpointRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = { "backchannel_authentication_endpoint_request_form_parameters", "authorization_endpoint_request" } )
	@PostEnvironment(required = "backchannel_authentication_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("authorization_endpoint_request");

		JsonObject o = env.getObject("backchannel_authentication_endpoint_request_form_parameters");

		for (String key : requestObjectClaims.keySet()) {
			JsonElement element = requestObjectClaims.get(key);

			// for nonce, state, client_id, redirect_uri, etc.
			if (element.isJsonPrimitive()) {

				o.addProperty(key, OIDFJSON.getString(element));
			}

		}
		env.putObject("backchannel_authentication_endpoint_request_form_parameters", o);

		log(o);

		return env;
	}
}
