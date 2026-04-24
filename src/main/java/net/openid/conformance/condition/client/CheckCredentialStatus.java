package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Decodes the Token Status List claim from the fetched status list JWT and
 * asserts the credential's status at the referenced index is VALID.
 *
 * Requires ExtractStatusListTokenFromStatusListTokenEndpointResponse to have
 * populated status_list_token and FetchStatusListToken to have populated
 * status_list_idx in the environment; runs after the signature of the status
 * list token has been verified (HAIP or non-HAIP path). Skips when no status
 * list fetch was performed (credential had no status claim).
 */
public class CheckCredentialStatus extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		if (!env.containsObject("status_list_token")) {
			log("No status list token in environment, skipping status check");
			return env;
		}

		String statusListTokenJwtString = env.getString("status_list_token", "value");
		if (statusListTokenJwtString == null) {
			throw error("Missing raw status list token JWT in environment");
		}

		Integer idx = env.getInteger("status_list_idx");
		if (idx == null) {
			throw error("Missing status_list_idx in environment");
		}

		JsonElement claimsElement = env.getElementFromObject("status_list_token", "claims");
		if (claimsElement == null || !claimsElement.isJsonObject()) {
			throw error("status_list_token is missing parsed claims");
		}

		JsonObject statusListTokenClaims = claimsElement.getAsJsonObject();
		JsonElement statusListElement = statusListTokenClaims.get("status_list");
		if (statusListElement == null || !statusListElement.isJsonObject()) {
			throw error("Missing status_list claim in StatusListToken");
		}

		JsonObject statusList = statusListElement.getAsJsonObject();

		TokenStatusList.Status status;
		try {
			if (!statusList.has("bits")) {
				throw error("Missing required 'bits' in status_list claim in StatusListToken", args("status_list", statusList));
			}

			if (!statusList.get("bits").isJsonPrimitive() || !statusList.get("bits").getAsJsonPrimitive().isNumber()) {
				throw error("Found invalid 'bits' in status_list claim in StatusListToken", args("status_list", statusList));
			}

			if (!statusList.has("lst")) {
				throw error("Missing 'lst' in status_list claim in StatusListToken", args("status_list", statusList));
			}

			if (!statusList.get("lst").isJsonPrimitive() || !statusList.get("lst").getAsJsonPrimitive().isString()) {
				throw error("Found invalid 'lst' in status_list claim in StatusListToken", args("status_list", statusList));
			}

			int bits = OIDFJSON.getInt(statusList.get("bits"));
			String lst = OIDFJSON.getString(statusList.get("lst"));

			TokenStatusList tokenStatusList = TokenStatusList.decode(lst, bits);
			status = tokenStatusList.getStatus(idx);

			if (!TokenStatusList.Status.VALID.equals(status)) {
				throw error("Detected invalid credential status in status_list. Status=" + status,
					args("status", status, "status_list_token_claims", statusListTokenClaims));
			}
		} catch (TokenStatusList.TokenStatusListException tsle) {
			throw error("Error decoding status_list token", tsle);
		}

		logSuccess("Found valid credential status in status_list. Status=" + status,
			args("status", status, "status_list_idx", idx));
		return env;
	}
}
