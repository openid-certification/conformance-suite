package io.fintechlabs.testframework.condition.client;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class GetDynamicServerConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public GetDynamicServerConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment, java.lang.String, io.fintechlabs.testframework.logging.EventLog)
	 */
	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("config")) {
			throw error("Couldn't find a configuration");
		}

		String staticIssuer = env.getString("config", "server.issuer");

		if (!Strings.isNullOrEmpty(staticIssuer)) {
			throw error("Static configuration element found, skipping dynamic server discovery", args("issuer", staticIssuer));
		}

		String discoveryUrl = env.getString("config", "server.discoveryUrl");

		if (Strings.isNullOrEmpty(discoveryUrl)) {

			String iss = env.getString("config", "server.discoveryIssuer");
			discoveryUrl = iss + "/.well-known/openid-configuration";

			if (Strings.isNullOrEmpty(iss)) {
				throw error("Couldn't find discoveryUrl or discoveryIssuer field for discovery purposes");
			}

		}

		// get out the server configuration component
		if (!Strings.isNullOrEmpty(discoveryUrl)) {
			// do an auto-discovery here

			// fetch the value
			String jsonString;
			try {
				RestTemplate restTemplate = createRestTemplate(env);
				jsonString = restTemplate.getForObject(discoveryUrl, String.class);
			} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
				throw error("Error creating HTTP client", e);
			} catch (RestClientResponseException e) {
				throw error("Unable to fetch server configuration from " + discoveryUrl, e);
			}

			if (!Strings.isNullOrEmpty(jsonString)) {
				log("Downloaded server configuration",
					args("server_config_string", jsonString));

				try {
					JsonObject serverConfig = new JsonParser().parse(jsonString).getAsJsonObject();

					logSuccess("Successfully parsed server configuration", serverConfig);

					env.putObject("server", serverConfig);

					return env;
				} catch (JsonSyntaxException e) {
					throw error(e);
				}

			} else {
				throw error("empty server configuration");
			}

		} else {
			throw error("Couldn't find or construct a discovery URL");
		}

	}

}
