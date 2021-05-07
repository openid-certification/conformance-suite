package net.openid.conformance.openbanking_brasil;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/*
	We endeavour to keep individual conditions as atomic as possible. Each one is presented
	as a step in the test UI. This condition takes the response injected by CallSimpleEndpoint
	and pulls the content out of it
 */
public class ExtractSimpleMessage extends AbstractCondition {

	@Override
	/*
		PreEnvironment lets us set preconditions for this evaluation
	 */
	@PreEnvironment(required = "resource_endpoint_response")
	@PostEnvironment(strings = "message")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("resource_endpoint_response");
		String message = OIDFJSON.getString(response.get("message"));
		env.putString("message", message);
		logSuccess("Message extracted");
		return env;
	}

}
