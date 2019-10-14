package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddRequestObjectClaimsToBackchannelAuthenticationEndpointRequest extends AbstractCondition {

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
