package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;

public class SetRequestUriParameterSupportedToTrueInServerConfiguration extends SetRequestParameterSupportedToTrueInServerConfiguration {

	@Override
	protected void addSupported(JsonObject server) {
		server.addProperty("request_uri_parameter_supported", true);
		server.addProperty("require_request_uri_registration", false);
	}

	@Override
	protected String getLogMessage() {
		return "Enabled request_uri support in server configuration";
	}
}
