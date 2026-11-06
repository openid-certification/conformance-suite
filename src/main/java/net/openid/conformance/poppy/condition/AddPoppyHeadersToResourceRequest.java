package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.poppy.PoppyConstants;
import net.openid.conformance.testmodule.Environment;

public class AddPoppyHeadersToResourceRequest extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "poppy_session_jwt")
	@PostEnvironment(required = "resource_endpoint_request_headers")
	public Environment evaluate(Environment env) {
		JsonObject headers = env.getObject("resource_endpoint_request_headers");
		if (headers == null) {
			headers = new JsonObject();
			env.putObject("resource_endpoint_request_headers", headers);
		}
		headers.addProperty(PoppyConstants.HEADER_SESSION, env.getString("poppy_session_jwt"));
		headers.addProperty(PoppyConstants.HEADER_VERSION, PoppyConstants.PROTOCOL_VERSION);

		logSuccess("Added the Poppy-Session and Poppy-Version headers to the resource request", args("headers", headers));
		return env;
	}
}
