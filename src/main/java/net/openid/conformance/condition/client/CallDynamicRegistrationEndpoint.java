package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
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

	protected boolean allowJsonParseFailure() {
		return false;
	}

	@Override
	@PreEnvironment(required = {"server", "dynamic_registration_request"})
	@PostEnvironment(required = "dynamic_registration_endpoint_response")
	public Environment evaluate(Environment env) {

		String registrationEndpoint = null;

		if (env.containsObject("mutual_tls_authentication")) {
			// for now, use the MTLS aliased endpoint if we have MTLS authentication available.
			// This is to cater for Brazil, where the DCR endpoint requires MTLS authentication.
			// It's not quite right for OIDC/CIBA cases if MTLS client authentication is in use -
			// the generic OAuth2 DCR endpoint shouldn't require MTLS.
			// I think here we could just call env.getString("server", "mtls_endpoint_aliases.registration_endpoint");
			// but https://gitlab.com/openid/conformance-suite/-/issues/914 is open to reconsider the overall
			// mechanism.
			registrationEndpoint = env.getString("registration_endpoint");
		}

		if (registrationEndpoint == null) {
			registrationEndpoint = env.getString("server", "registration_endpoint");
		}

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

			/*
			 * If there is an initial access token configured for the client include it in the authorization header.
			 *
			 * As per: https://openid.net/specs/openid-connect-registration-1_0.html#ClientRegistration
			 */
			String initialAccessToken = env.getString("initial_access_token");
			if (! Strings.isNullOrEmpty(initialAccessToken)){
				headers.set("Authorization", "Bearer " + initialAccessToken);
			}

			HttpEntity<?> request = new HttpEntity<>(requestObj.toString(), headers);

			try {
				ResponseEntity<String> response = restTemplate.exchange(registrationEndpoint, HttpMethod.POST, request, String.class);

				JsonObject responseInfo = convertJsonResponseForEnvironment("dynamic registration", response, allowJsonParseFailure());

				env.putObject("dynamic_registration_endpoint_response", responseInfo);

				log("Parsed registration endpoint response", responseInfo);

				return env;


			} catch (RestClientResponseException e) {
				throw error("RestClientResponseException occurred whilst calling registration endpoint",
					args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			} catch (RestClientException e) {
				return handleClientException(env, e);
			}

		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}
	}

	Environment handleClientException(Environment env, RestClientException e) {
		String msg = "Call to registration endpoint failed";
		if (e.getCause() != null) {
			msg += " - " +e.getCause().getMessage();
		}
		throw error(msg, e);
	}
}
