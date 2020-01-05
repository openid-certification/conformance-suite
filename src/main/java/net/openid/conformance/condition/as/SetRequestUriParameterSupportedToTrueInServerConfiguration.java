package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetRequestUriParameterSupportedToTrueInServerConfiguration extends SetRequestParameterSupportedToTrueInServerConfiguration {

	@Override
	protected void addSupported(JsonObject server) {
		server.addProperty("request_uri_parameter_supported", true);
	}

	@Override
	protected String getLogMessage() {
		return "Enabled request_uri support in server configuration";
	}
}
