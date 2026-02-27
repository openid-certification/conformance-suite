package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateRequestObjectHeader extends AbstractCondition {

	@Override
	@PostEnvironment(required = "request_object_header")
	public Environment evaluate(Environment env) {

		JsonObject requestObjectHeader = new JsonObject();

		env.putObject("request_object_header", requestObjectHeader);

		logSuccess("Created request object header", args("request_object_header", requestObjectHeader));

		return env;
	}

}
