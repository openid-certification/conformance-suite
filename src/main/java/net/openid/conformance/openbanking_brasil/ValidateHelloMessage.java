package net.openid.conformance.openbanking_brasil;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;

public class ValidateHelloMessage extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject response = environment.getObject("resource_endpoint_response");
		assertJsonField(response, "$.message", "Hello");
		logSuccess("Message was indeed 'Hello'");
		return environment;
	}

}
