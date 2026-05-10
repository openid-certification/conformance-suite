package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * If the token endpoint response includes RAR authorization_details (OID4VCI 8.2),
 * record the credential_identifiers per credential_configuration_id under
 * client.credential_identifiers_by_config_id so the credential request can use them
 * later, even after subsequent token-endpoint calls (e.g. negative tests in a
 * refresh-token plan) overwrite token_endpoint_response.
 *
 * No-op when the current token_endpoint_response has no authorization_details,
 * so any value previously recorded for the current client survives.
 */
public class VCIExtractCredentialIdentifiersFromTokenEndpointResponse extends AbstractCondition {

	/** RFC 9396 authorization_details {@code type} value defined by OID4VCI 1.0 Final §5.1.1. */
	public static final String RAR_OPENID_CREDENTIAL_TYPE = "openid_credential";

	/**
	 * Path under {@code client} where the per-credential_configuration_id identifier
	 * arrays are recorded. {@link VCIEnsureCredentialIdentifiersUnchangedAcrossTokenResponses}
	 * reads this BEFORE this condition writes to it (so the cross-response comparison
	 * sees the prior set, not the current one) — preserve the wiring order in
	 * {@code VCIProfileBehavior.afterTokenEndpointResponseProcessed}.
	 */
	public static final String CLIENT_ENV_KEY = "credential_identifiers_by_config_id";

	@Override
	@PreEnvironment(required = {"token_endpoint_response", "client"})
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		JsonElement authorizationDetailsEl = env.getElementFromObject("token_endpoint_response", "authorization_details");
		if (authorizationDetailsEl == null) {
			log("No authorization_details in token endpoint response, leaving previously recorded credential_identifiers (if any) untouched");
			return env;
		}

		if (!authorizationDetailsEl.isJsonArray()) {
			throw error("authorization_details in token endpoint response is not a JSON array",
				args("authorization_details", authorizationDetailsEl));
		}

		JsonElement existingEl = env.getElementFromObject("client", CLIENT_ENV_KEY);
		JsonObject byConfigId = existingEl != null && existingEl.isJsonObject()
			? existingEl.getAsJsonObject()
			: new JsonObject();

		boolean updated = false;
		for (JsonElement entryEl : authorizationDetailsEl.getAsJsonArray()) {
			if (!entryEl.isJsonObject()) {
				continue;
			}
			JsonObject entry = entryEl.getAsJsonObject();
			JsonElement typeEl = entry.get("type");
			if (!OIDFJSON.isString(typeEl) || !RAR_OPENID_CREDENTIAL_TYPE.equals(OIDFJSON.getString(typeEl))) {
				continue;
			}
			JsonElement configIdEl = entry.get("credential_configuration_id");
			JsonElement identifiersEl = entry.get("credential_identifiers");
			if (!OIDFJSON.isString(configIdEl) || identifiersEl == null || !identifiersEl.isJsonArray()) {
				continue;
			}
			byConfigId.add(OIDFJSON.getString(configIdEl), identifiersEl.getAsJsonArray());
			updated = true;
		}

		if (!updated) {
			log("authorization_details present but contained no openid_credential entries with credential_identifiers",
				args("authorization_details", authorizationDetailsEl));
			return env;
		}

		env.putObject("client", CLIENT_ENV_KEY, byConfigId);

		logSuccess("Recorded credential_identifiers under client",
			args("authorization_details", authorizationDetailsEl,
				CLIENT_ENV_KEY, byConfigId));
		return env;
	}
}
