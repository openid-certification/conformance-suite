package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
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
		addSignatureAlgValuesSupported(server);
		env.putObject("server", server);
		logSuccess(getLogMessage(), args("server", server));
		return env;
	}

	protected void addSignatureAlgValuesSupported(JsonObject server) {
		JsonArray signingAlgValuesSupported = new JsonArray();
		signingAlgValuesSupported.add("none");
		signingAlgValuesSupported.add("RS256");
		signingAlgValuesSupported.add("PS256");
		signingAlgValuesSupported.add("ES256");
		server.add("request_object_signing_alg_values_supported", signingAlgValuesSupported);
	}

	protected void addSupported(JsonObject server) {
		server.addProperty("request_parameter_supported", true);
	}

	protected String getLogMessage() {
		return "Enabled request parameter support in server configuration";
	}
}
