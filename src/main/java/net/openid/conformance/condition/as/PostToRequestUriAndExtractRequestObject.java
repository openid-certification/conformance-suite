package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jose.JOSEException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;

public class PostToRequestUriAndExtractRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request_params", "server_encryption_keys"})
	@PostEnvironment(required = {"authorization_request_object", "request_uri_post_response"})
	public Environment evaluate(Environment env) {
		String requestUri = env.getString("authorization_endpoint_http_request_params", "request_uri");
		if (!Strings.isNullOrEmpty(requestUri)) {
			log("POSTing to request_uri", args("request_uri", requestUri));
			String requestObjectString = "";
			JsonObject client = env.getObject("client");
			JsonObject serverEncKeys = env.getObject("server_encryption_keys");
			try {
				RestTemplate restTemplate = createRestTemplate(env);

				// Build form body
				MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();

				String walletNonce = env.getString("wallet_nonce");
				if (!Strings.isNullOrEmpty(walletNonce)) {
					formParams.add("wallet_nonce", walletNonce);
				}

				// Set headers
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				headers.set("Accept", "application/oauth-authz-req+jwt");

				HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formParams, headers);

				ResponseEntity<String> response = restTemplate.exchange(requestUri, HttpMethod.POST, requestEntity, String.class);
				requestObjectString = response.getBody();

				env.putObject("request_uri_post_response",
					convertResponseForEnvironment("request_uri", response));

				log("Downloaded request object via POST", args("request_object", requestObjectString));

				// request object will be decrypted if it's encrypted
				JsonObject jsonObjectForJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(requestObjectString, client, serverEncKeys);

				env.putObject("authorization_request_object", jsonObjectForJwt);

				logSuccess("Parsed request object", jsonObjectForJwt);

				return env;

			} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
				throw error("Error creating HTTP client", e);
			} catch (HttpStatusCodeException e) {
				ResponseEntity<String> errorResponse = ResponseEntity.status(e.getStatusCode())
					.headers(e.getResponseHeaders())
					.body(e.getResponseBodyAsString());
				env.putObject("request_uri_post_response",
					convertResponseForEnvironment("request_uri", errorResponse));
				throw error("Verifier responded with an HTTP error response when fetching request_uri. Per OID4VP §5.10.2 the wallet must terminate processing.",
					args("status", e.getStatusCode().value(), "request_uri", requestUri));
			} catch (RestClientException e) {
				String msg = "Unable to POST to request_uri at " + requestUri;
				if (e.getCause() != null) {
					msg += " - " + e.getCause().getMessage();
				}
				throw error(msg, e);
			} catch (JsonSyntaxException e) {
				throw error("Response is not JSON", e);
			} catch (ParseException e) {
				throw error("Couldn't parse request object", e, args("request", requestObjectString));
			} catch (JOSEException e) {
				throw error("Couldn't decrypt request object", e,
						args("request", requestObjectString, "keys", serverEncKeys));
			}

		} else {
			throw error("Authorization endpoint request does not contain a request_uri parameter");
		}

	}
}
