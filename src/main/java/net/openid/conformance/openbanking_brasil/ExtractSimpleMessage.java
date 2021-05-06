package net.openid.conformance.openbanking_brasil;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
	@PreEnvironment(required = "response")
	@PostEnvironment(strings = "message")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("response");
		String message = response.get("message").getAsString();
		env.putString("message", message);
		logSuccess("Message extracted");
		return env;
	}

}
