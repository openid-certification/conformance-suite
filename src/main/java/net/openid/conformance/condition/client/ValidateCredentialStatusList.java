package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.Map;

/**
 * Validates the credential's status via the Token Status List mechanism.
 *
 * Fetches the status list token from the URI in the credential's status claim,
 * verifies its signature, stores it in the environment as "status_list_token",
 * and checks the credential's status value.
 *
 * The caller should only invoke this condition when the credential contains
 * a status claim.
 */
public class ValidateCredentialStatusList extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(required = {"sdjwt"})
	public Environment evaluate(Environment env) {

		JsonElement statusEl = env.getElementFromObject("sdjwt", "credential.claims.status");
		if (statusEl == null) {
			log("No status claim found, skipping check");
			return env;
		}

		JsonObject statusClaimObj = statusEl.getAsJsonObject();
		if (!statusClaimObj.has("status_list")) {
			throw error("Missing status_list in status claim");
		}

		JsonObject statusListClaim = statusClaimObj.getAsJsonObject("status_list");
		if (!statusListClaim.has("idx")) {
			throw error("Missing idx in status_list in status claim");
		}

		if (!statusListClaim.has("uri")) {
			throw error("Missing uri in status_list in status claim");
		}

		int idx = OIDFJSON.getInt(statusListClaim.get("idx"));
		String uri = OIDFJSON.getString(statusListClaim.get("uri"));

		// Fetch the status list token
		ResponseEntity<String> statusListTokenJwtResponse;
		try {
			statusListTokenJwtResponse = fetchStatusListToken(env, uri);
		} catch (Exception e) {
			throw error("Unable to retrieve status list token from uri " + uri, e);
		}

		if (!statusListTokenJwtResponse.getStatusCode().is2xxSuccessful()) {
			throw error("Failed to retrieve status list token from uri " + uri, args("status", statusListTokenJwtResponse.getStatusCode()));
		}

		TokenStatusList.Status status;
		try {
			String statusListTokenJwtString = statusListTokenJwtResponse.getBody();

			// Store the status list token JWT in the environment for downstream conditions
			env.putObject("status_list_token", JWTUtil.jwtStringToJsonObjectForEnvironment(statusListTokenJwtString));

			JWTClaimsSet jwtClaimsSet = verifyAndParseStatusListToken(statusListTokenJwtString, env);

			Map<String, Object> statusList = jwtClaimsSet.getJSONObjectClaim("status_list");

			if (!statusList.containsKey("bits")) {
				throw error("Missing required 'bits' in status_list claim in StatusListToken", args("status_list", statusList));
			}

			if (!(statusList.get("bits") instanceof Long)) {
				throw error("Found invalid 'bits' in status_list claim in StatusListToken", args("status_list", statusList));
			}

			if (!statusList.containsKey("lst")) {
				throw error("Missing 'lst' in status_list claim in StatusListToken", args("status_list", statusList));
			}

			if (!(statusList.get("lst") instanceof String)) {
				throw error("Found invalid 'lst' in status_list claim in StatusListToken", args("status_list", statusList));
			}

			Long bits = (Long) statusList.get("bits");
			String lst = (String) statusList.get("lst");

			TokenStatusList tokenStatusList = TokenStatusList.decode(lst, bits.intValue());
			status = tokenStatusList.getStatus(idx);

			if (!TokenStatusList.Status.VALID.equals(status)) {
				throw error("Detected invalid credential status in status_list. Status=" + status, args("status", status, "status_list_token_claims", jwtClaimsSet));
			}

		} catch (ParseException e) {
			throw error("Unable to parse StatusList JWT token", e);
		} catch (TokenStatusList.TokenStatusListException tsle) {
			throw error("Error decoding status_list token", tsle);
		}

		logSuccess("Found valid credential status in status_list. Status=" + status, args("status_claim", statusClaimObj, "status", status, "status_list_idx", idx, "status_list_uri", uri));
		return env;
	}

	protected ResponseEntity<String> fetchStatusListToken(Environment env, String uri) throws Exception {
		RestTemplate restTemplate = createRestTemplate(env);
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, "application/statuslist+jwt");
		return restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
	}

	protected JWTClaimsSet verifyAndParseStatusListToken(String statusListTokenJwtString, Environment env) throws ParseException {
		SignedJWT statusListTokenJwt = SignedJWT.parse(statusListTokenJwtString);
		verifyStatusListTokenSignature(statusListTokenJwtString, statusListTokenJwt, env);
		return statusListTokenJwt.getJWTClaimsSet();
	}

	protected void verifyStatusListTokenSignature(String statusListTokenJwtString, SignedJWT statusListTokenJwt, Environment env) {
		if (statusListTokenJwt.getHeader().getJWK() != null) {
			JWKSet jwkSet = new JWKSet(statusListTokenJwt.getHeader().getJWK());
			JsonObject jwkSetObject = JsonParser.parseString(jwkSet.toString()).getAsJsonObject();
			verifyJwsSignature(statusListTokenJwtString, jwkSetObject, "status list token", false, "JWT header jwk");
			return;
		}

		if (env.containsObject("server_jwks")) {
			verifyJwsSignature(statusListTokenJwtString, env.getObject("server_jwks"), "status list token", false, "server");
			return;
		}

		throw error("Unable to verify status list token signature because neither an embedded JWK nor server_jwks is available",
			args("header", statusListTokenJwt.getHeader().toJSONObject()));
	}
}
