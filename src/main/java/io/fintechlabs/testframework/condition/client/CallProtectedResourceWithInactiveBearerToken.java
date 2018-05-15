package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.springframework.http.*;
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
import java.util.Map;

/**
 * @author srmoore
 * This is to call a generic resource server endpoint with a BAD Bearer Token.
 * We should return the env if we catch a 401 back from the resource server, and throw
 * a {@link io.fintechlabs.testframework.condition.ConditionError} if it comes back ok.
 */
public class CallProtectedResourceWithInactiveBearerToken extends AbstractCondition {

	public CallProtectedResourceWithInactiveBearerToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = { "access_token", "resource" })
	@PostEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("access_token", "value");
		if (Strings.isNullOrEmpty(accessToken)) {
			throw error("Access token not found");
		}

		String tokenType = env.getString("access_token", "type");
		if (Strings.isNullOrEmpty(tokenType)) {
			throw error("Token type not found");
		} else if (!tokenType.equalsIgnoreCase("Bearer")) {
			throw error("Access token is not a bearer token", args("token_type", tokenType));
		}

		String resourceUri = env.getString("resource", "resourceUrl");

		if(Strings.isNullOrEmpty(resourceUri)){
			throw error("Missing Resource URL");
		}

		HttpMethod resourceMethod = HttpMethod.GET;
		String configuredMethod = env.getString("resource","resourceMethod");
		if (!Strings.isNullOrEmpty(configuredMethod)) {
			resourceMethod = HttpMethod.valueOf(configuredMethod);
		}

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = new HttpHeaders();

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
			headers.setAcceptCharset(Collections.singletonList(Charset.forName("UTF-8")));
			headers.set("Authorization", String.join(" ", "Bearer", accessToken));

			HttpEntity<?> request = new HttpEntity<>(headers);

			ResponseEntity<String> response = restTemplate.exchange(resourceUri, resourceMethod, request, String.class);
			JsonObject responseCode = new JsonObject();
			responseCode.addProperty("code", response.getStatusCodeValue());


			throw error("Call to Resource Endpoint did not fail with bad token");

		}catch (RestClientResponseException e) {
			if(e.getRawStatusCode() >=400 && e.getRawStatusCode() < 500) {
				env.putString("resource_endpoint_response", e.getStatusText());
				logSuccess("Resource endpoint correctly rejected access token");
			} else {
				throw error("Error calling resource url", e);
			}
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		}
		return env;

	}
}
