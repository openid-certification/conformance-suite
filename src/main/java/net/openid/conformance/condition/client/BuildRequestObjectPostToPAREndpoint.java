package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * This class constructs the form encoded Request Object for a PAR request.
 */
public class BuildRequestObjectPostToPAREndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "request_object")
	@PostEnvironment(required = "pushed_authorization_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = new JsonObject();

		o.addProperty("request", env.getString("request_object"));

		env.putObject("pushed_authorization_request_form_parameters", o);

		logSuccess(o);

		return env;
	}

}
