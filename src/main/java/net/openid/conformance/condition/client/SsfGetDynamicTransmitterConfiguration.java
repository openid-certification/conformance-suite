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

public class SsfGetDynamicTransmitterConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = {"transmitter_metadata_endpoint_response", "transmitter_metadata"})
	public Environment evaluate(Environment env) {

		String iss = env.getString("config", "ssf.transmitter.issuer");
		if (!iss.endsWith("/")) {
			iss += "/";
		}
		String discoveryUrl = iss + ".well-known/ssf-configuration";

		if (Strings.isNullOrEmpty(iss)) {
			throw error("Couldn't find ssf.transmitter.issuer field for discovery purposes");
		}

		// get out the server configuration component
		if (!Strings.isNullOrEmpty(discoveryUrl)) {
			// do an auto-discovery here

			// fetch the value
			String jsonString;
			try {
				RestTemplate restTemplate = createRestTemplate(env);
				ResponseEntity<String> response = restTemplate.exchange(discoveryUrl, HttpMethod.GET, null, String.class);
				JsonObject responseInfo = convertResponseForEnvironment("ssf-configuration", response);

				env.putObject("transmitter_metadata_endpoint_response", responseInfo);

				jsonString = response.getBody();
			} catch (UnrecoverableKeyException | KeyManagementException | CertificateException |
					 InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
				throw error("Error creating HTTP client", e);
			} catch (RestClientException e) {
				String msg = "Unable to fetch server configuration from " + discoveryUrl;
				if (e.getCause() != null) {
					msg += " - " + e.getCause().getMessage();
				}
				throw error(msg, e);
			}

			if (!Strings.isNullOrEmpty(jsonString)) {
				try {
					JsonObject transmitterMetadata = JsonParser.parseString(jsonString).getAsJsonObject();

					logSuccess("Successfully parsed ssf configuration", transmitterMetadata);

					env.putObject("transmitter_metadata", transmitterMetadata);

					return env;
				} catch (JsonSyntaxException e) {
					throw error(e, args("json", jsonString));
				}

			} else {
				throw error("empty ssf configuration configuration");
			}

		} else {
			throw error("Couldn't find or construct a discovery URL");
		}

	}

}
