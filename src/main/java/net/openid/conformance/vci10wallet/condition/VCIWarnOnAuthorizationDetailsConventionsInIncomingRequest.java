package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Convention checks on the wallet's incoming authorization_details that aren't
 * strict spec MUST violations but are worth flagging as WARNING. Caller invokes
 * with WARNING severity.
 *
 * Findings collected:
 * - Unknown fields on an openid_credential entry. OID4VCI §5.1.1 defines
 *   type / credential_configuration_id / claims; RFC 9396 §2.2 adds the common
 *   locations / actions / datatypes / identifier / privileges fields.
 * - authorization_details entries of types other than openid_credential — RFC
 *   9396 §2 allows combining, but unexpected for a pure VCI flow.
 */
public class VCIWarnOnAuthorizationDetailsConventionsInIncomingRequest extends AbstractCondition {

	private static final Set<String> KNOWN_OPENID_CREDENTIAL_REQUEST_FIELDS = Set.of(
		// Defined for openid_credential by OID4VCI 1.0 Final §5.1.1
		"type", "credential_configuration_id", "claims",
		// RFC 9396 §2.2 common authorization-details fields — any type MAY include these
		"locations", "actions", "datatypes", "identifier", "privileges");

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	public Environment evaluate(Environment env) {

		JsonElement authorizationDetailsEl = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "authorization_details");
		if (authorizationDetailsEl == null || !authorizationDetailsEl.isJsonArray()) {
			log("authorization_details absent or not an array; nothing to inspect");
			return env;
		}

		List<String> nonOpenidCredentialTypes = new ArrayList<>();
		List<JsonObject> entriesWithUnknownFields = new ArrayList<>();

		for (JsonElement entryEl : authorizationDetailsEl.getAsJsonArray()) {
			if (!entryEl.isJsonObject()) {
				continue;
			}
			JsonObject entry = entryEl.getAsJsonObject();
			JsonElement typeEl = entry.get("type");
			String type = OIDFJSON.isString(typeEl) ? OIDFJSON.getString(typeEl) : null;

			if (!VCIValidateOpenidCredentialAuthorizationDetailsInIncomingRequest.RAR_OPENID_CREDENTIAL_TYPE.equals(type)) {
				if (type != null) {
					nonOpenidCredentialTypes.add(type);
				}
				continue;
			}

			List<String> unknownFields = new ArrayList<>();
			for (String key : entry.keySet()) {
				if (!KNOWN_OPENID_CREDENTIAL_REQUEST_FIELDS.contains(key)) {
					unknownFields.add(key);
				}
			}
			if (!unknownFields.isEmpty()) {
				JsonObject report = new JsonObject();
				report.add("entry", entry);
				JsonArray fieldsArr = new JsonArray();
				unknownFields.forEach(fieldsArr::add);
				report.add("unknown_fields", fieldsArr);
				entriesWithUnknownFields.add(report);
			}
		}

		boolean hasFindings = !nonOpenidCredentialTypes.isEmpty() || !entriesWithUnknownFields.isEmpty();
		if (hasFindings) {
			throw error("authorization_details in incoming request contains conventions issues",
				args(
					"non_openid_credential_types_present", nonOpenidCredentialTypes,
					"openid_credential_entries_with_unknown_fields", entriesWithUnknownFields));
		}

		logSuccess("authorization_details in incoming request uses only known openid_credential fields");
		return env;
	}
}
