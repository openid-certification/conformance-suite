package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Per OID4VCI 1.0 Final §5.1.1:
 *   "If the Credential Issuer metadata contains an authorization_servers
 *    parameter, the authorization detail's locations common data field
 *    MUST be set to the Credential Issuer Identifier value."
 *
 * This condition checks the rule on the wallet's incoming authorization request,
 * when the suite-as-issuer advertises authorization_servers in its metadata.
 * Skips when the issuer metadata doesn't advertise authorization_servers
 * (locations is then OPTIONAL).
 */
public class VCIEnsureLocationsMatchesCredentialIssuerWhenAuthorizationServersAdvertised extends AbstractCondition {

	@Override
	@PreEnvironment(required = {CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "credential_issuer_metadata"})
	public Environment evaluate(Environment env) {

		JsonElement authorizationServersEl = env.getElementFromObject("credential_issuer_metadata", "authorization_servers");
		if (authorizationServersEl == null) {
			log("authorization_servers is not advertised by the Credential Issuer metadata — locations is OPTIONAL, skipping check");
			return env;
		}

		String credentialIssuer = env.getString("credential_issuer");
		if (credentialIssuer == null || credentialIssuer.isEmpty()) {
			throw error("Credential Issuer Identifier is not set in env — cannot evaluate locations rule");
		}

		JsonElement authorizationDetailsEl = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "authorization_details");
		if (authorizationDetailsEl == null || !authorizationDetailsEl.isJsonArray()) {
			throw error("authorization_details missing from incoming request or not a JSON array",
				args("authorization_details", authorizationDetailsEl));
		}

		boolean checkedAtLeastOneEntry = false;
		for (JsonElement entryEl : authorizationDetailsEl.getAsJsonArray()) {
			if (!entryEl.isJsonObject()) {
				continue;
			}
			JsonObject entry = entryEl.getAsJsonObject();
			JsonElement typeEl = entry.get("type");
			if (!OIDFJSON.isString(typeEl) || !VCIValidateOpenidCredentialAuthorizationDetailsInIncomingRequest.RAR_OPENID_CREDENTIAL_TYPE
					.equals(OIDFJSON.getString(typeEl))) {
				continue;
			}

			JsonElement locationsEl = entry.get("locations");
			if (locationsEl == null) {
				throw error("openid_credential authorization_details entry is missing 'locations' — the Credential Issuer advertises authorization_servers, so locations MUST be present and equal to the Credential Issuer Identifier per OID4VCI 1.0 Final §5.1.1",
					args("entry", entry, "credential_issuer", credentialIssuer));
			}
			if (!locationsEl.isJsonArray()) {
				throw error("'locations' is not a JSON array", args("entry", entry, "locations", locationsEl));
			}
			JsonArray locations = locationsEl.getAsJsonArray();
			if (locations.size() != 1) {
				throw error("'locations' must contain exactly the Credential Issuer Identifier (one entry) per OID4VCI 1.0 Final §5.1.1",
					args("entry", entry, "locations", locations, "credential_issuer", credentialIssuer));
			}
			JsonElement firstLocationEl = locations.get(0);
			if (!OIDFJSON.isString(firstLocationEl) || !credentialIssuer.equals(OIDFJSON.getString(firstLocationEl))) {
				throw error("'locations' value does not equal the Credential Issuer Identifier per OID4VCI 1.0 Final §5.1.1",
					args("entry", entry, "locations", locations, "expected", credentialIssuer));
			}
			checkedAtLeastOneEntry = true;
		}

		if (!checkedAtLeastOneEntry) {
			log("No openid_credential entries to check");
			return env;
		}

		logSuccess("'locations' values match the Credential Issuer Identifier on all openid_credential entries",
			args("credential_issuer", credentialIssuer));
		return env;
	}
}
