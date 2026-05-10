package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashMap;
import java.util.Map;

/**
 * For each credential_configuration_id present in BOTH the current token endpoint
 * response's authorization_details AND the previously-recorded set under
 * client.credential_identifiers_by_config_id, the multiset of credential_identifiers
 * MUST be the same.
 *
 * Tracks the suggestion in https://github.com/openid/OpenID4VCI/issues/739 ahead of
 * normative spec language.
 *
 * Must run BEFORE VCIExtractCredentialIdentifiersFromTokenEndpointResponse so the
 * comparison is against identifiers from the previous token response, not the current
 * one. No-op on the first call (no prior set stored) and on entries whose
 * credential_configuration_id is not in the prior set.
 */
public class VCIEnsureCredentialIdentifiersUnchangedAcrossTokenResponses extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"token_endpoint_response", "client"})
	public Environment evaluate(Environment env) {

		JsonElement authorizationDetailsEl = env.getElementFromObject("token_endpoint_response", "authorization_details");
		if (authorizationDetailsEl == null || !authorizationDetailsEl.isJsonArray()) {
			log("authorization_details absent or not an array; nothing to compare");
			return env;
		}

		JsonElement priorEl = env.getElementFromObject("client",
			VCIExtractCredentialIdentifiersFromTokenEndpointResponse.CLIENT_ENV_KEY);
		if (priorEl == null || !priorEl.isJsonObject()) {
			log("No prior credential_identifiers recorded; nothing to compare");
			return env;
		}
		JsonObject prior = priorEl.getAsJsonObject();

		for (JsonElement entryEl : authorizationDetailsEl.getAsJsonArray()) {
			if (!entryEl.isJsonObject()) {
				continue;
			}
			JsonObject entry = entryEl.getAsJsonObject();
			JsonElement typeEl = entry.get("type");
			if (!OIDFJSON.isString(typeEl) || !VCIExtractCredentialIdentifiersFromTokenEndpointResponse.RAR_OPENID_CREDENTIAL_TYPE.equals(OIDFJSON.getString(typeEl))) {
				continue;
			}
			JsonElement configIdEl = entry.get("credential_configuration_id");
			JsonElement identifiersEl = entry.get("credential_identifiers");
			if (!OIDFJSON.isString(configIdEl) || identifiersEl == null || !identifiersEl.isJsonArray()) {
				continue;
			}
			String configId = OIDFJSON.getString(configIdEl);
			JsonElement priorIdsEl = prior.get(configId);
			if (priorIdsEl == null || !priorIdsEl.isJsonArray()) {
				continue;
			}

			JsonArray currentIds = identifiersEl.getAsJsonArray();
			JsonArray priorIds = priorIdsEl.getAsJsonArray();
			if (!sameMultiset(currentIds, priorIds)) {
				throw error("credential_identifiers for this credential_configuration_id differ from a previous token endpoint response",
					args("credential_configuration_id", configId,
						"current_credential_identifiers", currentIds,
						"previous_credential_identifiers", priorIds));
			}
		}

		logSuccess("credential_identifiers unchanged across token endpoint responses for all credential_configuration_ids seen before");
		return env;
	}

	private static boolean sameMultiset(JsonArray a, JsonArray b) {
		if (a.size() != b.size()) {
			return false;
		}
		Map<String, Integer> counts = new HashMap<>();
		for (JsonElement el : a) {
			if (!OIDFJSON.isString(el)) {
				return false;
			}
			counts.merge(OIDFJSON.getString(el), 1, Integer::sum);
		}
		for (JsonElement el : b) {
			if (!OIDFJSON.isString(el)) {
				return false;
			}
			Integer remaining = counts.computeIfPresent(OIDFJSON.getString(el), (k, v) -> v - 1);
			if (remaining == null || remaining < 0) {
				return false;
			}
		}
		for (Integer v : counts.values()) {
			if (v != 0) {
				return false;
			}
		}
		return true;
	}
}
