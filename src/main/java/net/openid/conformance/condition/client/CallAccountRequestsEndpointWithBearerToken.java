package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking.FAPIOBGetResourceEndpoint;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;


public class CallAccountRequestsEndpointWithBearerToken extends AbstractCondition {

	private static final String ACCOUNT_REQUESTS_RESOURCE_V2 = "account-requests";
	// As per https://openbanking.atlassian.net/wiki/spaces/DZ/pages/937820271/Account+and+Transaction+API+Specification+-+v3.1
	private static final String ACCOUNT_REQUESTS_RESOURCE_V3 = "account-access-consents";
	private static final String ACCOUNT_REQUESTS_RESOURCE_V4 = "account-access-consents";

	@Override
	@PreEnvironment(required = { "access_token", "resource", "account_requests_endpoint_request", "resource_endpoint_request_headers" })
	@PostEnvironment(required = { "resource_endpoint_response_headers", "account_requests_endpoint_response" })
	public Environment evaluate(Environment env) {
		// Note that this code doesn't follow the prefered pattern and should be refactored; extending CallProtectedResource is prefered

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

		String resourceEndpoint = FAPIOBGetResourceEndpoint.getBaseResourceURL(env, FAPIOBGetResourceEndpoint.Endpoint.ACCOUNT_REQUESTS);
		if (Strings.isNullOrEmpty(resourceEndpoint)) {
			throw error("Resource endpoint not found");
		}

		// Check which OB API version is used in the configuration by inspecting the url
		// As per https://openbanking.atlassian.net/wiki/spaces/DZ/pages/937656404/Read%2BWrite%2BData%2BAPI%2BSpecification%2B-%2Bv3.1
		// Resource URI Path Structure
		// The path of the URI must follow the structure below (from the OB API Release Management document).
		// [participant-path-prefix]/open-banking/[version]/[resource-group]/[resource]/[resource-id]/[sub-resource]
		// [version] The version of the APIs expressed as /v[major-version].[minor-version]/.
		String urlPath;
		if (resourceEndpoint.contains("/v4.")) {
			urlPath = ACCOUNT_REQUESTS_RESOURCE_V4;
			env.putInteger("ob_api_version", 4);
			log("Found '/v4.' in the resource url, using OB V4 API '"+urlPath+"'", args("resource_endpoint", resourceEndpoint));
		} else if (resourceEndpoint.contains("/v3.")) {
			urlPath = ACCOUNT_REQUESTS_RESOURCE_V3;
			env.putInteger("ob_api_version", 3);
			log("Found '/v3.' in the resource url, using OB V3 API '"+urlPath+"'", args("resource_endpoint", resourceEndpoint));
		}  else {
			urlPath = ACCOUNT_REQUESTS_RESOURCE_V2;
			env.putInteger("ob_api_version", 2);
			log("'/v3.' or '/v4.' not found in the resource url, defaulting to OB V1/V2 API '"+urlPath+"'", args("resource_endpoint", resourceEndpoint));
		}

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		JsonObject requestObject = env.getObject("account_requests_endpoint_request");
		if (requestObject == null) {
			throw error("Couldn't find request object");
		}

		// Build the endpoint URL
		String accountRequestsUrl = UriComponentsBuilder.fromUriString(resourceEndpoint)
			.path(urlPath)
			.toUriString();

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = headersFromJson(requestHeaders);

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", "Bearer " + accessToken);


			HttpEntity<String> request = new HttpEntity<>(requestObject.toString(), headers);

			ResponseEntity<String> response = restTemplate.exchange(accountRequestsUrl, HttpMethod.POST, request, String.class);

			String jsonString = response.getBody();

			if (Strings.isNullOrEmpty(jsonString)) {
				throw error("Empty/missing response from the account requests endpoint");
			} else {
				log("Account requests endpoint response", args("account_requests_endpoint_response", jsonString));

				try {
					JsonElement jsonRoot = JsonParser.parseString(jsonString);
					if (jsonRoot == null || !jsonRoot.isJsonObject()) {
						throw error("Account requests endpoint did not return a JSON object");
					}

					JsonObject responseHeaders = mapToJsonObject(response.getHeaders(), true); // lowercase incoming headers

					env.putObject("account_requests_endpoint_response", jsonRoot.getAsJsonObject());
					env.putObject("resource_endpoint_response_headers", responseHeaders);

					logSuccess("Parsed account requests endpoint response", args("body", jsonString, "headers", responseHeaders));

					return env;
				} catch (JsonParseException e) {
					throw error(e);
				}
			}
		} catch (RestClientResponseException e) {
			throw error("Error from the account requests endpoint", args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		} catch (RestClientException e) {
			String msg = "Call to account requests endpoint " + resourceEndpoint + " failed";
			if (e.getCause() != null) {
				msg += " - " +e.getCause().getMessage();
			}
			throw error(msg, e);
		}

	}

}
