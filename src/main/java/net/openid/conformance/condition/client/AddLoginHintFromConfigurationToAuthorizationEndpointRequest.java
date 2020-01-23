package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;

public class AddLoginHintFromConfigurationToAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "config" } )
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String loginHint = env.getString("config", "server.login_hint");
		String msg;
		if (Strings.isNullOrEmpty(loginHint)) {
			String issuer = env.getString("server", "issuer");
			try {
				@SuppressWarnings("unused")
				URI issuerUri = new URI(issuer);

				loginHint = "buffy@"+issuerUri.getHost();
			} catch (URISyntaxException e) {
				throw error("Couldn't parse issuer as URL", e, args("issuer", issuer));
			}

			msg = "No login_hint in test configuration, created one based on issuer and added login_hint to authorization endpoint request";
		} else {
			msg = "Added login_hint from test configuration to authorization endpoint request";
		}

		authorizationEndpointRequest.addProperty("login_hint", loginHint);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess(msg, authorizationEndpointRequest);

		return env;

	}

}
