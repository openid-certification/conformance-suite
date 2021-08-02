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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
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

public class CallDynamicRegistrationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server", "dynamic_registration_request"})
	@PostEnvironment(required = "dynamic_registration_endpoint_response")
	public Environment evaluate(Environment env) {

		String registrationEndpoint = env.getString("server", "registration_endpoint");
		if (registrationEndpoint == null) {
			throw error("Couldn't find registration endpoint");
		}

		JsonObject requestObj = env.getObject("dynamic_registration_request");

		try {

			RestTemplate restTemplate = createRestTemplate(env);

			restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
				@Override
				public boolean hasError(ClientHttpResponse response) throws IOException {
					// Treat all http status codes as 'not an error', so spring never throws an exception due to the http
					// status code meaning the rest of our code can handle http status codes how it likes
					return false;
				}
			});

			HttpHeaders headers = new HttpHeaders();

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<?> request = new HttpEntity<>(requestObj.toString(), headers);

			String jsonString = null;

			try {
				ResponseEntity<String> response = restTemplate.exchange(registrationEndpoint, HttpMethod.POST, request, String.class);
				JsonObject responseInfo = convertResponseForEnvironment("dynamic registration", response);

				jsonString = response.getBody();
				if (Strings.isNullOrEmpty(jsonString)) {
					throw error("Empty response from the registration endpoint");
				}

				log("Registration endpoint response", args("dynamic_registration_response", jsonString));

				try {
					JsonElement jsonRoot = new JsonParser().parse(jsonString);
					if (jsonRoot == null || !jsonRoot.isJsonObject()) {
						throw error("Registration Endpoint did not return a JSON object");
					}

					JsonObject client = jsonRoot.getAsJsonObject();

					responseInfo.add("body_json", client);
					log("Parsed registration endpoint response", responseInfo);

					env.putObject("dynamic_registration_endpoint_response", responseInfo);
					return env;
				} catch (JsonParseException e) {
					throw error("Response from dynamic registration endpoint does not appear to be a JSON object", e);
				}

			} catch (RestClientResponseException e) {
				throw error("RestClientResponseException occurred whilst calling registration endpoint",
					e, args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			} catch (RestClientException e) {
				String msg = "Call to registration endpoint " + registrationEndpoint + " failed";
				if (e.getCause() != null) {
					msg += " - " +e.getCause().getMessage();
				}
				throw error(msg, e);
			}

		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}
	}
}
