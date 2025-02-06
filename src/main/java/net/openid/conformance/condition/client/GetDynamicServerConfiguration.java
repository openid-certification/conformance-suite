package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

public class GetDynamicServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = { "server", "discovery_endpoint_response" } )
	public Environment evaluate(Environment env) {

		if (!env.containsObject("config")) {
			throw error("Couldn't find a configuration");
		}

		String staticIssuer = env.getString("config", "server.issuer");

		if (!Strings.isNullOrEmpty(staticIssuer)) {
			throw error("Test set to use dynamic server configuration but test configuration contains static server configuration", args("issuer", staticIssuer));
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
				ResponseEntity<String> response = restTemplate.exchange(discoveryUrl, HttpMethod.GET, null, String.class);
				JsonObject responseInfo = convertResponseForEnvironment("discovery", response);

				env.putObject("discovery_endpoint_response", responseInfo);

				jsonString = response.getBody();
			} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
				throw error("Error creating HTTP client", e);
			} catch (RestClientException e) {
				String msg = "Unable to fetch server configuration from " + discoveryUrl;
				if (e.getCause() != null) {
					msg += " - " +e.getCause().getMessage();
				}
				throw error(msg, e);
			}

			if (!Strings.isNullOrEmpty(jsonString)) {
				try {
					JsonObject serverConfig = JsonParser.parseString(jsonString).getAsJsonObject();

					logSuccess("Successfully parsed server configuration", serverConfig);

					env.putObject("server", serverConfig);

					return env;
				} catch (JsonSyntaxException e) {
					throw error(e, args("json", jsonString));
				}

			} else {
				throw error("empty server configuration");
			}

		} else {
			throw error("Couldn't find or construct a discovery URL");
		}

	}

}
