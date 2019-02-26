package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
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

public class CallBackchannelAuthenticationEndpoint extends AbstractCondition {

	private static final Logger logger = LoggerFactory.getLogger(CallBackchannelAuthenticationEndpoint.class);

	public CallBackchannelAuthenticationEndpoint(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = { "server", "backchannel_authentication_endpoint_request_form_parameters" })
	@PostEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {

		final String bcAuthEndpoint = env.getString("server", "backchannel_authentication_endpoint");
		if (bcAuthEndpoint == null) {
			throw error("Couldn't find backchannel authentication endpoint");
		}

		// build up the form
		JsonObject formJson = env.getObject("backchannel_authentication_endpoint_request_form_parameters");
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (String key : formJson.keySet()) {
			form.add(key, formJson.get(key).getAsString());
		}

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = headersFromJson(env.getObject("backchannel_authentication_endpoint_request_headers"));

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
			headers.setAcceptCharset(Collections.singletonList(Charset.forName("UTF-8")));

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

			String jsonString = null;

			try {
				jsonString = restTemplate.postForObject(bcAuthEndpoint, request, String.class);
			} catch (RestClientResponseException e) {
				throw error("Error from the backchannel authentication endpoint", e, args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			}

			if (Strings.isNullOrEmpty(jsonString)) {
				throw error("Got an empty response from the backchannel authentication endpoint");
			} else {
				log("Backchannel Authentication endpoint response",
					args("backchannel_authentication_endpoint_response", jsonString));

				try {
					JsonElement jsonRoot = new JsonParser().parse(jsonString);
					if (jsonRoot == null || !jsonRoot.isJsonObject()) {
						throw error("Backchannel Authentication Endpoint did not return a JSON object");
					}

					logSuccess("Parsed backchannel authentication endpoint response", jsonRoot.getAsJsonObject());

					env.putObject("backchannel_authentication_endpoint_response", jsonRoot.getAsJsonObject());

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
