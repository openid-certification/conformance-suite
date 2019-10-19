package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;

public class CallDynamicRegistrationEndpoint extends AbstractCondition {

	private static final Logger logger = LoggerFactory.getLogger(CallDynamicRegistrationEndpoint.class);

	@Override
	@PreEnvironment(required = {"server", "dynamic_registration_request"})
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		if (env.getString("server", "registration_endpoint") == null) {
			throw error("Couldn't find registration endpoint");
		}

		if (!env.containsObject("dynamic_registration_request")){
			throw error("Couldn't find dynamic registration request");
		}

		JsonObject requestObj = env.getObject("dynamic_registration_request");

		try {

			RestTemplate restTemplate = createRestTemplate(env);
			HttpHeaders headers = new HttpHeaders();

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.setAcceptCharset(Collections.singletonList(Charset.forName("UTF-8")));
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<?> request = new HttpEntity<>(requestObj.toString(), headers);

			String jsonString = null;

			try {
				jsonString = restTemplate.postForObject(env.getString("server", "registration_endpoint"), request, String.class);
			} catch (RestClientResponseException e) {
				throw error("Error from the dynamic registration endpoint", e, args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			}

			if (Strings.isNullOrEmpty(jsonString)) {
				throw error("Didn't get back a response from the registration endpoint");
			} else {
				log("Registration endpoint response", args("dynamic_registration_response", jsonString));

				try {
					JsonElement jsonRoot = new JsonParser().parse(jsonString);
					if (jsonRoot == null || !jsonRoot.isJsonObject()) {
						throw error("Registration Endpoint did not return a JSON object");
					}

					JsonObject client = jsonRoot.getAsJsonObject();
					logSuccess("Parsed registration endpoint response", client);

					env.putObject("client", client);

					if (client.has("registration_client_uri") &&
						client.has("registration_access_token")) {

						String registrationClientUri = OIDFJSON.getString(client.get("registration_client_uri"));
						String registrationAccessToken = OIDFJSON.getString(client.get("registration_access_token"));

						if (!Strings.isNullOrEmpty(registrationClientUri) &&
							!Strings.isNullOrEmpty(registrationAccessToken)) {
							env.putString("registration_client_uri", registrationClientUri);
							env.putString("registration_access_token", registrationAccessToken);

							logSuccess("Extracted dynamic registration management credentials",
								args("registration_client_uri", registrationClientUri,
									"registration_access_token", registrationAccessToken));
						}
					}
					return env;
				} catch (JsonParseException e) {
					throw error(e);
				}
			}
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			logger.warn("Error creating HTTP Client", e);
			throw error("Error creating HTTP Client", e);
		}
	}
}
