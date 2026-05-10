package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

/**
 * FAILURE-grade structural checks on type=openid_credential authorization_details entries
 * in a token endpoint response. Per OID4VCI 1.0 Final §5.1.1 (entry shape) and §6.2
 * (credential_identifiers).
 *
 * The generic RAR-level checks (presence, array shape, per-entry type attribute) are
 * handled by RARSupport.CheckForAuthorizationDetailsInTokenResponse and are not repeated
 * here.
 */
public class VCIValidateOpenidCredentialAuthorizationDetailsInTokenEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"token_endpoint_response", "vci"})
	public Environment evaluate(Environment env) {

		JsonElement authorizationDetailsEl = env.getElementFromObject("token_endpoint_response", "authorization_details");
		if (authorizationDetailsEl == null || !authorizationDetailsEl.isJsonArray()) {
			throw error("authorization_details missing from token endpoint response or not a JSON array",
				args("authorization_details", authorizationDetailsEl));
		}

		String requestedConfigId = env.getString("vci_credential_configuration_id");

		JsonArray entries = authorizationDetailsEl.getAsJsonArray();
		Set<String> seenConfigIds = new HashSet<>();
		boolean foundMatchingConfigId = false;

		for (JsonElement entryEl : entries) {
			if (!entryEl.isJsonObject()) {
				throw error("authorization_details entry is not a JSON object", args("entry", entryEl));
			}
			JsonObject entry = entryEl.getAsJsonObject();
			JsonElement typeEl = entry.get("type");
			if (typeEl == null) {
				throw error("authorization_details entry missing 'type'", args("entry", entry));
			}
			if (!OIDFJSON.isString(typeEl)) {
				throw error("authorization_details entry 'type' is not a string", args("entry", entry));
			}
			if (!VCIExtractCredentialIdentifiersFromTokenEndpointResponse.RAR_OPENID_CREDENTIAL_TYPE.equals(OIDFJSON.getString(typeEl))) {
				continue;
			}

			JsonElement configIdEl = entry.get("credential_configuration_id");
			if (!OIDFJSON.isString(configIdEl)) {
				throw error("openid_credential authorization_details entry missing or non-string 'credential_configuration_id'",
					args("entry", entry));
			}
			String configId = OIDFJSON.getString(configIdEl);
			if (configId.isEmpty()) {
				throw error("openid_credential authorization_details entry has empty 'credential_configuration_id'",
					args("entry", entry));
			}
			if (!seenConfigIds.add(configId)) {
				throw error("Multiple authorization_details entries share the same credential_configuration_id",
					args("credential_configuration_id", configId, "authorization_details", entries));
			}

			JsonElement identifiersEl = entry.get("credential_identifiers");
			if (identifiersEl == null) {
				throw error("openid_credential authorization_details entry missing 'credential_identifiers'",
					args("entry", entry));
			}
			if (!identifiersEl.isJsonArray()) {
				throw error("openid_credential authorization_details entry 'credential_identifiers' is not a JSON array",
					args("entry", entry));
			}
			JsonArray identifiers = identifiersEl.getAsJsonArray();
			if (identifiers.isEmpty()) {
				throw error("openid_credential authorization_details entry has empty 'credential_identifiers' array",
					args("entry", entry));
			}
			for (JsonElement idEl : identifiers) {
				if (!OIDFJSON.isString(idEl)) {
					throw error("credential_identifiers contains a non-string value",
						args("entry", entry, "credential_identifiers", identifiers));
				}
				if (OIDFJSON.getString(idEl).isEmpty()) {
					throw error("credential_identifiers contains an empty string",
						args("entry", entry, "credential_identifiers", identifiers));
				}
			}

			if (configId.equals(requestedConfigId)) {
				foundMatchingConfigId = true;
			}
		}

		if (!foundMatchingConfigId) {
			throw error("No authorization_details entry of type 'openid_credential' has a credential_configuration_id matching the requested credential",
				args("requested_credential_configuration_id", requestedConfigId, "authorization_details", entries));
		}

		logSuccess("authorization_details openid_credential entries are well-formed and include the requested credential_configuration_id",
			args("requested_credential_configuration_id", requestedConfigId, "authorization_details", entries));
		return env;
	}
}
