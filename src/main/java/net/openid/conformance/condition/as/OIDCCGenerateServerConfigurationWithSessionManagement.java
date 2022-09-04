package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;

public class OIDCCGenerateServerConfigurationWithSessionManagement extends OIDCCGenerateServerConfiguration {

	@Override
	protected void addAdditionalConfiguration(JsonObject server, String baseUrl) {
		server.addProperty("check_session_iframe", baseUrl + "check_session_iframe");
		server.addProperty("end_session_endpoint", baseUrl + "end_session_endpoint");
		server.addProperty("frontchannel_logout_supported", true);
		server.addProperty("frontchannel_logout_session_supported", true);
		server.addProperty("backchannel_logout_supported", true);
		server.addProperty("backchannel_logout_session_supported", true);

	}
}
