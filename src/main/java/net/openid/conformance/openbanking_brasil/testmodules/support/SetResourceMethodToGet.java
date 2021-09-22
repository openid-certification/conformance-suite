package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetResourceMethodToGet extends AbstractCondition {

	@Override
	@PostEnvironment(required = "resource")
	public Environment evaluate(Environment env) {
		JsonObject resource = env.getObject("resource");
		if (resource == null) {
			resource = new JsonObject();
		}
		resource.remove("resourceMethod");
		resource.addProperty("resourceMethod", "GET");
		env.putObject("resource", resource);
		logSuccess("Set protected resource access method to GET");

		return env;
	}

}
