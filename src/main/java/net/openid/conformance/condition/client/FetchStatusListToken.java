package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Fetches the Token Status List JWT referenced by the credential's status claim
 * and stores the raw HTTP response in the environment for downstream
 * validation conditions.
 *
 * Stores: status_list_token_endpoint_response, status_list_idx,
 * status_list_uri.
 *
 * Clears any stale status list state from a previous credential before
 * evaluating the current credential. If the credential has no status claim,
 * logs a skip message and returns without fetching anything.
 */
public class FetchStatusListToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = "sdjwt")
	public Environment evaluate(Environment env) {
		clearStatusListState(env);

		JsonElement statusEl = env.getElementFromObject("sdjwt", "credential.claims.status");
		if (statusEl == null) {
			log("No status claim found, skipping status list check");
			return env;
		}

		JsonObject statusClaimObj = statusEl.getAsJsonObject();
		if (!statusClaimObj.has("status_list")) {
			throw error("Missing status_list in status claim", args("status_claim", statusClaimObj));
		}

		JsonObject statusListClaim = statusClaimObj.getAsJsonObject("status_list");
		if (!statusListClaim.has("idx")) {
			throw error("Missing idx in status_list in status claim", args("status_claim", statusClaimObj));
		}

		if (!statusListClaim.has("uri")) {
			throw error("Missing uri in status_list in status claim", args("status_claim", statusClaimObj));
		}

		int idx = OIDFJSON.getInt(statusListClaim.get("idx"));
		String uri = OIDFJSON.getString(statusListClaim.get("uri"));

		ResponseEntity<String> statusListTokenJwtResponse;
		try {
			statusListTokenJwtResponse = fetchStatusListToken(env, uri);
		} catch (Exception e) {
			throw error("Unable to retrieve status list token from uri " + uri, e);
		}

		env.putObject("status_list_token_endpoint_response",
			convertResponseForEnvironment("status list token endpoint", statusListTokenJwtResponse));

		if (!statusListTokenJwtResponse.getStatusCode().is2xxSuccessful()) {
			throw error("Failed to retrieve status list token from uri " + uri,
				args("status", statusListTokenJwtResponse.getStatusCode()));
		}

		env.putInteger("status_list_idx", idx);
		env.putString("status_list_uri", uri);

		logSuccess("Fetched status list token", args("status_list_uri", uri, "status_list_idx", idx));
		return env;
	}

	private void clearStatusListState(Environment env) {
		env.removeObject("status_list_token_endpoint_response");
		env.removeObject("status_list_token");
		env.removeNativeValue("status_list_idx");
		env.removeNativeValue("status_list_uri");
	}

	protected ResponseEntity<String> fetchStatusListToken(Environment env, String uri) throws Exception {
		RestTemplate restTemplate = createRestTemplate(env);
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, "application/statuslist+jwt");
		return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
	}
}
