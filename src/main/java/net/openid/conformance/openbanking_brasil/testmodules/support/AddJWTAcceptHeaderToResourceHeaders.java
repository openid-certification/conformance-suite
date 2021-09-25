package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddJWTAcceptHeaderToResourceHeaders  extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"resource_endpoint_request_headers" })
	public Environment evaluate(Environment env) {
		JsonObject headers = env.getObject("resource_endpoint_request_headers");
		headers.addProperty("accept_type", "application/jwt");
		log("Adding jwt to accept header");
		return env;
	}

}
