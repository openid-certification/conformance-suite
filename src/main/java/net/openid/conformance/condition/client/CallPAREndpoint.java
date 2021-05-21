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
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

/**
 * This class makes a http post to PAR endpoint and the response is stored in the ENV
 */
public class CallPAREndpoint extends AbstractCondition {

	public static final String HTTP_METHOD_KEY = "par_endpoint_http_method";

	private static final Logger logger = LoggerFactory.getLogger(CallPAREndpoint.class);

	@Override
	@PreEnvironment(required = {"server", "pushed_authorization_request_form_parameters"})
	@PostEnvironment(required = {"pushed_authorization_endpoint_response", "pushed_authorization_endpoint_response_headers"})
	public Environment evaluate(Environment env) {

		// build up the form
		JsonObject formJson = env.getObject("pushed_authorization_request_form_parameters");
		MultiValueMap <String, String> form = new LinkedMultiValueMap <>();
		for (String key : formJson.keySet()) {
			form.add(key, OIDFJSON.getString(formJson.get(key)));
		}

		//Subclasses may add additional form parameters if any
		addAdditionalParams(form);

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(DATAUTILS_MEDIATYPE_APPLICATION_JSON_UTF8));
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));

			HttpEntity <MultiValueMap <String, String>> request = new HttpEntity <>(form, headers);

			String jsonString = null;
			HttpMethod httpMethod = env.getString(HTTP_METHOD_KEY) == null ?
				HttpMethod.POST : HttpMethod.valueOf(env.getString(HTTP_METHOD_KEY));

			try {
				final String parEndpointUri = env.getString("server", "pushed_authorization_request_endpoint");
				if (Strings.isNullOrEmpty(parEndpointUri)) {
					throw error("Couldn't find pushed_authorization_request_endpoint in server discovery document. This endpoint is required as you have selected to test pushed authorization requests.");
				}

				restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
					@Override
					public boolean hasError(ClientHttpResponse response) throws IOException {
						// Treat all http status codes as 'not an error', so spring never throws an exception due to the http
						// status code meaning the rest of our code can handle http status codes how it likes
						return false;
					}
				});

				ResponseEntity <String> response = restTemplate
					.exchange(parEndpointUri, httpMethod, request, String.class);

				logSuccess("Storing pushed_authorization_endpoint_response_http_status " + response.getStatusCode().value());

				env.putInteger("pushed_authorization_endpoint_response_http_status", response.getStatusCodeValue());

				JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true);

				env.putObject("pushed_authorization_endpoint_response_headers", responseHeaders);

				jsonString = response.getBody();

			} catch (RestClientResponseException e) {
				throw error("RestClientResponseException occurred whilst calling pushed authorization request endpoint",
					e, args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			} catch (RestClientException e) {
				return handleResponseException(env, e);
			}

			if (!httpMethod.equals(HttpMethod.POST)) {
				env.putObject("pushed_authorization_endpoint_response", new JsonObject());
				return env;
			}

			if (Strings.isNullOrEmpty(jsonString)) {
				throw error("Missing or empty response from the pushed authorization request endpoint");
			}

			try {
				JsonElement jsonRoot = new JsonParser().parse(jsonString);
				if (jsonRoot == null || !jsonRoot.isJsonObject()) {
					throw error("Pushed Authorization did not return a JSON object");
				}

				logSuccess("Parsed pushed authorization request endpoint response", jsonRoot.getAsJsonObject());

				env.putObject("pushed_authorization_endpoint_response", jsonRoot.getAsJsonObject());

				return env;
			} catch (JsonParseException e) {
				throw error(e);
			}
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			logger.warn("Error creating HTTP Client", e);
			throw error("Error creating HTTP Client", e);
		}
	}

	protected void addAdditionalParams(MultiValueMap <String, String> form) {
		//do nothing by default
	}

	protected Environment handleResponseException(Environment env, RestClientException e) {
		String msg = "Call to pushed authorization request endpoint failed";
		if (e.getCause() != null) {
			msg += " - " + e.getCause().getMessage();
		}
		throw error(msg, e);
	}
}
