package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Convention checks on token endpoint response authorization_details that aren't strict
 * spec MUST violations but are worth flagging. Caller invokes with WARNING severity so
 * each non-empty findings array surfaces as a single warning entry in the test log.
 *
 * Findings collected:
 * - openid_credential entries whose credential_configuration_id was not in the request.
 *   §6.1.1 ¶5 only RECOMMENDS subset acceptance; supersets are not addressed.
 * - Duplicate values within a single entry's credential_identifiers array. §6.2 says
 *   each identifier "uniquely identifies" a Credential Dataset.
 * - authorization_details entries of types other than openid_credential when our
 *   request only contained openid_credential. §5.1.1 ¶13 allows combining types but
 *   unrequested ones are suspicious.
 * - Unknown fields on an openid_credential entry (anything other than type,
 *   credential_configuration_id, credential_identifiers, claims). §5.1.1 ¶4 allows
 *   extensions, but per the project's sender-vs-receiver guidance the issuer should
 *   not include undefined fields.
 */
public class VCIWarnOnAuthorizationDetailsInTokenEndpointResponseConventions extends AbstractCondition {

	private static final Set<String> KNOWN_OPENID_CREDENTIAL_FIELDS = Set.of(
		// Defined for openid_credential by OID4VCI 1.0 Final §5.1.1 / §6.2
		"type", "credential_configuration_id", "credential_identifiers", "claims",
		// RFC 9396 §2.2 common authorization-details fields — any type MAY include these
		"locations", "actions", "datatypes", "identifier", "privileges");

	@Override
	@PreEnvironment(required = {"token_endpoint_response", "vci"})
	public Environment evaluate(Environment env) {

		JsonElement authorizationDetailsEl = env.getElementFromObject("token_endpoint_response", "authorization_details");
		if (authorizationDetailsEl == null || !authorizationDetailsEl.isJsonArray()) {
			log("authorization_details absent or not an array; nothing to inspect");
			return env;
		}

		Set<String> requestedConfigIds = collectRequestedConfigIds(env);
		JsonArray entries = authorizationDetailsEl.getAsJsonArray();

		List<String> unrequestedConfigIds = new ArrayList<>();
		List<JsonObject> entriesWithDuplicateIdentifiers = new ArrayList<>();
		List<String> unrequestedTypes = new ArrayList<>();
		List<JsonObject> entriesWithUnknownFields = new ArrayList<>();
		// Cross-entry duplicate detection only matters with 2+ openid_credential entries.
		Map<String, Integer> identifierCountsAcrossEntries = entries.size() > 1 ? new HashMap<>() : null;

		for (JsonElement entryEl : entries) {
			if (!entryEl.isJsonObject()) {
				continue;
			}
			JsonObject entry = entryEl.getAsJsonObject();
			JsonElement typeEl = entry.get("type");
			String type = OIDFJSON.isString(typeEl) ? OIDFJSON.getString(typeEl) : null;

			if (!VCIExtractCredentialIdentifiersFromTokenEndpointResponse.RAR_OPENID_CREDENTIAL_TYPE.equals(type)) {
				if (type != null) {
					unrequestedTypes.add(type);
				}
				continue;
			}

			JsonElement configIdEl = entry.get("credential_configuration_id");
			if (OIDFJSON.isString(configIdEl)) {
				String configId = OIDFJSON.getString(configIdEl);
				if (!requestedConfigIds.isEmpty() && !requestedConfigIds.contains(configId)) {
					unrequestedConfigIds.add(configId);
				}
			}

			JsonElement identifiersEl = entry.get("credential_identifiers");
			if (identifiersEl != null && identifiersEl.isJsonArray()) {
				JsonArray identifiers = identifiersEl.getAsJsonArray();
				if (hasDuplicateStrings(identifiers)) {
					entriesWithDuplicateIdentifiers.add(entry);
				}
				if (identifierCountsAcrossEntries != null) {
					for (JsonElement idEl : identifiers) {
						if (OIDFJSON.isString(idEl)) {
							identifierCountsAcrossEntries.merge(OIDFJSON.getString(idEl), 1, Integer::sum);
						}
					}
				}
			}

			List<String> unknownFields = new ArrayList<>();
			for (String key : entry.keySet()) {
				if (!KNOWN_OPENID_CREDENTIAL_FIELDS.contains(key)) {
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

		List<String> identifiersAppearingInMultipleEntries = new ArrayList<>();
		if (identifierCountsAcrossEntries != null) {
			identifierCountsAcrossEntries.forEach((id, count) -> {
				if (count > 1) {
					identifiersAppearingInMultipleEntries.add(id);
				}
			});
		}

		boolean hasFindings = !unrequestedConfigIds.isEmpty()
			|| !entriesWithDuplicateIdentifiers.isEmpty()
			|| !unrequestedTypes.isEmpty()
			|| !entriesWithUnknownFields.isEmpty()
			|| !identifiersAppearingInMultipleEntries.isEmpty();
		if (hasFindings) {
			throw error("authorization_details in token endpoint response contains conventions issues",
				args(
					"unrequested_credential_configuration_ids", unrequestedConfigIds,
					"entries_with_duplicate_credential_identifiers", entriesWithDuplicateIdentifiers,
					"unrequested_authorization_details_types", unrequestedTypes,
					"openid_credential_entries_with_unknown_fields", entriesWithUnknownFields,
					"credential_identifiers_appearing_in_multiple_entries", identifiersAppearingInMultipleEntries,
					"requested_credential_configuration_ids", requestedConfigIds));
		}

		logSuccess("authorization_details in token endpoint response is consistent with the request and known fields");
		return env;
	}

	private static Set<String> collectRequestedConfigIds(Environment env) {
		Set<String> requested = new HashSet<>();
		String configured = env.getString("vci_credential_configuration_id");
		if (configured != null && !configured.isEmpty()) {
			requested.add(configured);
		}
		// Also pick up anything from the constructed RAR (covers future multi-credential cases)
		JsonElement rarPayloadEl = env.getElementFromObject("rar", "payload");
		if (rarPayloadEl != null && rarPayloadEl.isJsonArray()) {
			for (JsonElement el : rarPayloadEl.getAsJsonArray()) {
				if (!el.isJsonObject()) {
					continue;
				}
				JsonElement configIdEl = el.getAsJsonObject().get("credential_configuration_id");
				if (OIDFJSON.isString(configIdEl)) {
					requested.add(OIDFJSON.getString(configIdEl));
				}
			}
		}
		return requested;
	}

	private static boolean hasDuplicateStrings(JsonArray array) {
		Set<String> seen = new HashSet<>();
		for (JsonElement el : array) {
			if (!OIDFJSON.isString(el)) {
				continue;
			}
			if (!seen.add(OIDFJSON.getString(el))) {
				return true;
			}
		}
		return false;
	}
}
