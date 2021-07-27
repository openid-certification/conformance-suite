package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CallDynamicRegistrationEndpoint extends AbstractCallDynamicRegistrationEndpoint {

	@Override
	@PreEnvironment(required = {"server", "dynamic_registration_request"})
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		return callDynamicRegistrationEndpoint(env);
	}

	@Override
	protected Environment onRegistrationEndpointResponse(Environment env, JsonObject client) {
		return env;
	}

	@Override
	protected Environment onRegistrationEndpointError(Environment env, Throwable e, int code, String status, String body) {

		throw error("Error from the dynamic registration endpoint", e, args("code", code, "status", status, "body", body));
	}
}
