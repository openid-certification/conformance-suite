package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class SetResponseStatus extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {

		JsonObject response = env.getObject("consent_endpoint_response_full");
		env.putInteger("resource_endpoint_response_status", OIDFJSON.getInt(response.get("status")));

		return env;
	}
}
