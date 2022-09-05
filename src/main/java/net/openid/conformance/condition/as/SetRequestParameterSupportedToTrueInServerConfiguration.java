package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetRequestParameterSupportedToTrueInServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonObject server = env.getObject("server");
		addSupported(server);
		log(getLogMessage(), args("server", server));
		return env;
	}

	protected void addSupported(JsonObject server) {
		server.addProperty("request_parameter_supported", true);
	}

	protected String getLogMessage() {
		return "Enabled request parameter support in server configuration";
	}
}
