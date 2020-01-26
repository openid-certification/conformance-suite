package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;

public abstract class AbstractCallDynamicRegistrationEndpoint extends AbstractCondition {

	private static final Logger logger = LoggerFactory.getLogger(AbstractCallDynamicRegistrationEndpoint.class);

	protected Environment callDynamicRegistrationEndpoint(Environment env) {

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
			headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<?> request = new HttpEntity<>(requestObj.toString(), headers);

			String jsonString = null;

			try {
				jsonString = restTemplate.postForObject(env.getString("server", "registration_endpoint"), request, String.class);
			} catch (RestClientResponseException e) {
				return onRegistrationEndpointError(env, e, e.getRawStatusCode(), e.getStatusText(), e.getResponseBodyAsString());
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
					log("Parsed registration endpoint response", client);

					return onRegistrationEndpointResponse(env, client);
				} catch (JsonParseException e) {
					throw error(e);
				}
			}
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			logger.warn("Error creating HTTP Client", e);
			throw error("Error creating HTTP Client", e);
		}
	}

	protected abstract Environment onRegistrationEndpointResponse(Environment env, JsonObject response);

	protected abstract Environment onRegistrationEndpointError(Environment env, Throwable e, int code, String status, String body);

}
