package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

public class CallRevocationEndpoint extends AbstractCondition {

	private static final Logger logger = LoggerFactory.getLogger(CallRevocationEndpoint.class);

	@Override
	@PreEnvironment(required = { "server", "revocation_endpoint_request_form_parameters" })
	public Environment evaluate(Environment env) {
		if (env.getString("server", "revocation_endpoint") == null) {
			throw error("Couldn't find revocation endpoint");
		}

		if (!env.containsObject("revocation_endpoint_request_form_parameters")) {
			throw error("Couldn't find request form");
		}

		JsonObject formJson = env.getObject("revocation_endpoint_request_form_parameters");
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (String key : formJson.keySet()) {
			form.add(key, OIDFJSON.getString(formJson.get(key)));
		}

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			// extract the headers for use (below)
			HttpHeaders headers = headersFromJson(env.getObject("revocation_endpoint_request_headers"));

			headers.setAccept(Collections.singletonList(DATAUTILS_MEDIATYPE_APPLICATION_JSON_UTF8));
			headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));

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
