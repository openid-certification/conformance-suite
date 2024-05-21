package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

public class CallRPBackChannelLogoutEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "server", "client" })
	@PostEnvironment(required = "backchannel_logout_endpoint_response")
	public Environment evaluate(Environment env) {

		MultiValueMap<String, String> formParameters = new LinkedMultiValueMap<>();
		formParameters.put("logout_token", List.of(env.getString("logout_token")));
		formParameters.put("ignored_parameter",
							List.of("The POST body MAY contain other values in addition to logout_token. " +
									"Values that are not understood by the implementation MUST be ignored."));
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

			try {
				final String logoutEndpointUri = env.getString("client", "backchannel_logout_uri");
				HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formParameters, null);
				ResponseEntity<String> response = restTemplate.exchange(logoutEndpointUri, HttpMethod.POST, request, String.class);
				JsonObject responseInfo = convertResponseForEnvironment("backchannel logout", response);

				env.putObject("backchannel_logout_endpoint_response", responseInfo);

				logSuccess("Called backchannel_logout_uri", args("backchannel_logout_endpoint_response", responseInfo));
			} catch (RestClientResponseException e) {
				throw error("RestClientResponseException occurred whilst calling logout endpoint",
					args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			} catch (RestClientException e) {
				return handleClientException(env, e);
			}
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}
		return env;
	}

	protected Environment handleClientException(Environment env, RestClientException e) {
		throw error("RestClientException happened whilst calling logout endpoint", ex(e));
	}

}
