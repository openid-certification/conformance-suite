package net.openid.conformance.vci10issuer.condition.statuslist;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.Map;

public class VCIValidateCredentialValidityByStatusListIfPresent extends AbstractCondition {

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
			throw error("Missing status_list found in status claim");
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
		// validate token status via status list from uri with idx
		// see: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#section-8.3

		// fetch referenced token_status list token
		ResponseEntity<String> statusListTokenJwtResponse;
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			HttpEntity<String> httpEntity = new HttpEntity<>(MultiValueMap.fromSingleValue(Map.of(HttpHeaders.ACCEPT, "application/statuslist+jwt")));
			statusListTokenJwtResponse = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);
		} catch (Exception e) {
			throw error("Unable to retrieve statuslist token from uri " + uri, e);
		}

		if (!statusListTokenJwtResponse.getStatusCode().is2xxSuccessful()) {
			throw error("Failed to retrieve statuslist token from uri " + uri, args("status", statusListTokenJwtResponse.getStatusCode()));
		}

		TokenStatusList.Status status;
		// Status List in JSON Format, see: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#section-4.2
		try {
			// TODO validate token status list token
			String statusListTokenJwtString = statusListTokenJwtResponse.getBody();

			JWT statusListTokenJwt = JWTUtil.parseJWT(statusListTokenJwtString);

			// extract token status list
			JWTClaimsSet jwtClaimsSet = statusListTokenJwt.getJWTClaimsSet();

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

			// ensure token based on index is valid according to TokenStatusList
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
}
