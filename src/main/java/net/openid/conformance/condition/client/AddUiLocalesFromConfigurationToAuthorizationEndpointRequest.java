package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddUiLocalesFromConfigurationToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "config" } )
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String uiLocales = env.getString("config", "server.ui_locales");
		String msg;
		if (Strings.isNullOrEmpty(uiLocales)) {
			uiLocales = "se";

			msg = "No ui_locales in test configuration, added ui_locales=se to authorization endpoint request";
		} else {
			msg = "Added ui_locales from test configuration to authorization endpoint request";
		}

		authorizationEndpointRequest.addProperty("ui_locales", uiLocales);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess(msg, authorizationEndpointRequest);

		return env;

	}

}
