package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * This class constructs the form encoded Request Object for a PAR request.
 * This is only to be used for private_key_jwt case and not for MTLS.
 */
public class BuildRequestObjectPostToPAREndpoint extends AbstractCondition {

	public static final String CLIENT_ASSERTION = "client_assertion";
	@Override
	@PreEnvironment(strings = {"request_object", CLIENT_ASSERTION})
	@PostEnvironment(required = "pushed_authorization_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = new JsonObject();
		o.addProperty("request", env.getString("request_object"));
		o.addProperty(CLIENT_ASSERTION, env.getString("client_assertion"));
		o.addProperty("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

		env.putObject("pushed_authorization_request_form_parameters", o);

		logSuccess(o);

		return env;
	}

}
