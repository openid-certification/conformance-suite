package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.util.TLSTestValueExtractor;
import net.openid.conformance.testmodule.Environment;

import java.net.MalformedURLException;

public class ExtractTLSTestValuesFromServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = {"token_endpoint_tls"}) // always required, others are added as found: authorization_endpoint_tls, userinfo_endpoint_tls, registration_endpoint_tls
	public Environment evaluate(Environment env) {

		try {
			String authorizationEndpoint = env.getString("authorization_endpoint") != null ? env.getString("authorization_endpoint") : env.getString("server", "authorization_endpoint");

			JsonObject authorizationEndpointTls = null;
			if (!Strings.isNullOrEmpty(authorizationEndpoint)) {
				authorizationEndpointTls = TLSTestValueExtractor.extractTlsFromUrl(authorizationEndpoint);
				env.putObject("authorization_endpoint_tls", authorizationEndpointTls);
			}

			String tokenEndpoint = env.getString("token_endpoint") != null ? env.getString("token_endpoint") : env.getString("server", "token_endpoint");
			if (Strings.isNullOrEmpty(tokenEndpoint)) {
				throw error("Token endpoint not found");
			}

			JsonObject tokenEndpointTls = TLSTestValueExtractor.extractTlsFromUrl(tokenEndpoint);
			env.putObject("token_endpoint_tls", tokenEndpointTls);

			String userInfoEndpoint = env.getString("userinfo_endpoint") != null ? env.getString("userinfo_endpoint") : env.getString("server", "userinfo_endpoint");
			JsonObject userInfoEndpointTls = null;
			if (!Strings.isNullOrEmpty(userInfoEndpoint)) {
				userInfoEndpointTls = TLSTestValueExtractor.extractTlsFromUrl(userInfoEndpoint);
				env.putObject("userinfo_endpoint_tls", userInfoEndpointTls);
			}

			String registrationEndpoint = env.getString("registration_endpoint") != null ? env.getString("registration_endpoint") : env.getString("server", "registration_endpoint");
			JsonObject registrationEndpointTls = null;
			if (!Strings.isNullOrEmpty(registrationEndpoint)) {
				registrationEndpointTls = TLSTestValueExtractor.extractTlsFromUrl(registrationEndpoint);
				env.putObject("registration_endpoint_tls", registrationEndpointTls);
			}

			logSuccess("Extracted TLS information from authorization server configuration", args(
					"authorization_endpoint", authorizationEndpointTls,
					"token_endpoint", tokenEndpointTls,
					"userinfo_endpoint", userInfoEndpointTls,
					"registration_endpoint", registrationEndpointTls
				));

			return env;
		} catch (MalformedURLException e) {
			throw error("URL not properly formed", e);
		}

	}


}
