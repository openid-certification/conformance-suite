package io.fintechlabs.testframework.condition.client;

import java.net.MalformedURLException;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.util.TLSTestValueExtractor;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractTLSTestValuesFromServerConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ExtractTLSTestValuesFromServerConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = {"authorization_endpoint_tls", "token_endpoint_tls"}) // these two are required, others are added as found: userinfo_endpoint_tls, registration_endpoint_tls
	public Environment evaluate(Environment env) {

		try {
			String authorizationEndpoint = env.getString("server", "authorization_endpoint");
			if (Strings.isNullOrEmpty(authorizationEndpoint)) {
				throw error("Authorization endpoint not found");
			}

			JsonObject authorizationEndpointTls = TLSTestValueExtractor.extractTlsFromUrl(authorizationEndpoint);
			env.putObject("authorization_endpoint_tls", authorizationEndpointTls);

			String tokenEndpoint = env.getString("server", "token_endpoint");
			if (Strings.isNullOrEmpty(tokenEndpoint)) {
				throw error("Token endpoint not found");
			}

			JsonObject tokenEndpointTls = TLSTestValueExtractor.extractTlsFromUrl(tokenEndpoint);
			env.putObject("token_endpoint_tls", tokenEndpointTls);

			String userInfoEndpoint = env.getString("server", "userinfo_endpoint");
			JsonObject userInfoEndpointTls = null;
			if (!Strings.isNullOrEmpty(userInfoEndpoint)) {
				userInfoEndpointTls = TLSTestValueExtractor.extractTlsFromUrl(userInfoEndpoint);
				env.putObject("userinfo_endpoint_tls", userInfoEndpointTls);
			}

			String registrationEndpoint = env.getString("server", "registration_endpoint");
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
