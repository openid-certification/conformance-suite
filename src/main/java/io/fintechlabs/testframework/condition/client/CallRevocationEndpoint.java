package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

/**
 * @author srmoore
 */
public class CallRevocationEndpoint extends AbstractCondition {

	private static final Logger logger = LoggerFactory.getLogger(CallRevocationEndpoint.class);

	public CallRevocationEndpoint(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = { "server", "revocation_endpoint_request_form_parameters" })
	public Environment evaluate(Environment env) {
		if (env.getString("server", "revocation_endpoint") == null) {
			throw error("Couldn't find revocation endpoint");
		}

		if (!env.containsObj("revocation_endpoint_request_form_parameters")) {
			throw error("Couldn't find request form");
		}

		JsonObject formJson = env.get("revocation_endpoint_request_form_parameters");
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (String key : formJson.keySet()) {
			form.add(key, formJson.get(key).getAsString());
		}

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			// extract the headers for use (below)
			HttpHeaders headers = new HttpHeaders();

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
			headers.setAcceptCharset(Collections.singletonList(Charset.forName("UTF-8")));

			// Not sure we need this block.
			JsonObject headersJson = env.get("revocation_endpoint_request_headers");
			if (headersJson != null) {
				for (String header : headersJson.keySet()) {
					headers.set(header, headersJson.get(header).getAsString());
				}
			}

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

			String jsonString = null;

			try {
				jsonString = restTemplate.postForObject(env.getString("server", "revocation_endpoint"), request, String.class);
			} catch (RestClientResponseException e) {
				throw error("Error from the revocation endpoint", e, args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			}

			logSuccess("Called Revocation Endpoint");
			return env;
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
				logger.warn("Error creating HTTP Client", e);
				throw error("Error creating HTTP Client", e);
		}
	}
}
