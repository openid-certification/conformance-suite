package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * FAILURE-grade structural checks on type=openid_credential authorization_details
 * entries in the wallet's incoming authorization request (whether sent directly or
 * via PAR — both end up in effective_authorization_endpoint_request).
 *
 * Per OID4VCI 1.0 Final §5.1.1: type MUST be "openid_credential" (for this spec's
 * usage); credential_configuration_id is REQUIRED and must identify a Credential
 * in the issuer's credential_configurations_supported map. If claims is present,
 * it must be a non-empty array of claims description objects per Appendix B.1
 * — each with a required non-empty 'path' array whose components are strings,
 * nulls, or integers per Appendix C, and (if present) a boolean 'mandatory'.
 *
 * credential_identifiers is response-only (§6.2) and MUST NOT appear in the
 * request — that case is flagged here as a structural failure.
 */
public class VCIValidateOpenidCredentialAuthorizationDetailsInIncomingRequest extends AbstractCondition {

	public static final String RAR_OPENID_CREDENTIAL_TYPE = "openid_credential";

	@Override
	@PreEnvironment(required = {CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "credential_issuer_metadata"})
	public Environment evaluate(Environment env) {

		JsonElement authorizationDetailsEl = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "authorization_details");
		if (authorizationDetailsEl == null || !authorizationDetailsEl.isJsonArray()) {
			throw error("authorization_details missing from incoming request or not a JSON array",
				args("authorization_details", authorizationDetailsEl));
		}

		JsonElement supportedEl = env.getElementFromObject("credential_issuer_metadata", "credential_configurations_supported");
		if (supportedEl == null || !supportedEl.isJsonObject()) {
			throw error("credential_issuer_metadata.credential_configurations_supported missing or not a JSON object",
				args("credential_configurations_supported", supportedEl));
		}
		JsonObject supported = supportedEl.getAsJsonObject();

		JsonArray entries = authorizationDetailsEl.getAsJsonArray();
		Set<String> seenConfigIds = new HashSet<>();
		boolean sawOpenidCredential = false;

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
			if (!RAR_OPENID_CREDENTIAL_TYPE.equals(OIDFJSON.getString(typeEl))) {
				continue;
			}
			sawOpenidCredential = true;

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
			if (!supported.has(configId)) {
				throw error("credential_configuration_id is not advertised in the issuer's credential_configurations_supported metadata, per OID4VCI 1.0 Final §5.1.1",
					args("credential_configuration_id", configId, "credential_configurations_supported", supported.keySet()));
			}

			validateClaimsIfPresent(entry);

			if (entry.has("credential_identifiers")) {
				throw error("Wallet sent 'credential_identifiers' in the request — this parameter is response-only per OID4VCI 1.0 Final §6.2",
					args("entry", entry));
			}
		}

		if (!sawOpenidCredential) {
			throw error("authorization_details contained no entries of type 'openid_credential'",
				args("authorization_details", entries));
		}

		logSuccess("Incoming request authorization_details openid_credential entries are well-formed",
			args("authorization_details", entries));
		return env;
	}

	private void validateClaimsIfPresent(JsonObject entry) {
		JsonElement claimsEl = entry.get("claims");
		if (claimsEl == null) {
			return;
		}
		if (!claimsEl.isJsonArray()) {
			throw error("openid_credential authorization_details 'claims' is present but not a JSON array, per OID4VCI 1.0 Final §5.1.1",
				args("entry", entry, "claims", claimsEl));
		}
		JsonArray claims = claimsEl.getAsJsonArray();
		if (claims.isEmpty()) {
			throw error("openid_credential authorization_details 'claims' is present but is an empty array; the spec requires a non-empty array of claims description objects per OID4VCI 1.0 Final §5.1.1",
				args("entry", entry));
		}
		for (JsonElement claimEl : claims) {
			if (!claimEl.isJsonObject()) {
				throw error("'claims' entry is not a JSON object — each entry must be a claims description object per OID4VCI 1.0 Final Appendix B.1",
					args("entry", entry, "claim", claimEl));
			}
			validateClaimsDescriptionObject(entry, claimEl.getAsJsonObject());
		}
	}

	private void validateClaimsDescriptionObject(JsonObject entry, JsonObject claim) {
		JsonElement pathEl = claim.get("path");
		if (pathEl == null) {
			throw error("claims description object is missing required 'path' per OID4VCI 1.0 Final Appendix B.1",
				args("entry", entry, "claim", claim));
		}
		if (!pathEl.isJsonArray()) {
			throw error("claims description object 'path' is not a JSON array per OID4VCI 1.0 Final Appendix B.1",
				args("entry", entry, "claim", claim, "path", pathEl));
		}
		JsonArray path = pathEl.getAsJsonArray();
		if (path.isEmpty()) {
			throw error("claims description object 'path' is an empty array, but OID4VCI 1.0 Final Appendix B.1 requires a non-empty array",
				args("entry", entry, "claim", claim));
		}
		for (JsonElement pathComponent : path) {
			if (!isValidPathComponent(pathComponent)) {
				throw error("claims description object 'path' contains an invalid element — each component must be a string, null, or integer per OID4VCI 1.0 Final Appendix C",
					args("entry", entry, "claim", claim, "invalid_path_component", pathComponent));
			}
		}

		JsonElement mandatoryEl = claim.get("mandatory");
		if (mandatoryEl != null
			&& !(mandatoryEl.isJsonPrimitive() && mandatoryEl.getAsJsonPrimitive().isBoolean())) {
			throw error("claims description object 'mandatory' is not a boolean per OID4VCI 1.0 Final Appendix B.1",
				args("entry", entry, "claim", claim, "mandatory", mandatoryEl));
		}
	}

	private static boolean isValidPathComponent(JsonElement el) {
		if (el == null) {
			return false;
		}
		if (el.isJsonNull()) {
			return true;
		}
		if (!el.isJsonPrimitive()) {
			return false;
		}
		JsonPrimitive p = el.getAsJsonPrimitive();
		if (p.isString()) {
			return true;
		}
		if (!p.isNumber()) {
			return false;
		}
		try {
			return new BigDecimal(OIDFJSON.forceConversionToString(p)).stripTrailingZeros().scale() <= 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
