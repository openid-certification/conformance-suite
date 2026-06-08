package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * Per IETF SD-JWT VC draft-13 §9.3: "The mandatory property is a boolean
 * indicating that, if set to true, the claim MUST be included in the
 * credential by the Issuer."
 *
 * Resolves each {@code claims[i]} entry whose {@code mandatory} is true
 * against the Processed SD-JWT Payload at {@code sdjwt.decoded}. The §9.1
 * claim-path semantics are implemented in full:
 * <ul>
 *   <li>string — select that named object member.</li>
 *   <li>non-negative integer — select that array index.</li>
 *   <li>null — wildcard over an array; selects every element.</li>
 * </ul>
 * A wildcard path that addresses zero elements (e.g. an empty array) is
 * treated as a mandatory failure: the spec requires the addressed claims to
 * be included in the credential, and "no claims included" cannot satisfy
 * that requirement.
 */
public class VCIEnsureMandatoryClaimsArePresent extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "vci"})
	public Environment evaluate(Environment env) {
		JsonElement payloadEl = env.getElementFromObject("sdjwt", "decoded");
		if (payloadEl == null || !payloadEl.isJsonObject()) {
			throw error("Processed SD-JWT payload not found at sdjwt.decoded");
		}
		JsonObject payload = payloadEl.getAsJsonObject();

		JsonElement claimsEl = env.getElementFromObject("vci", "sdjwt_vc_type_metadata.claims");
		if (claimsEl == null || !claimsEl.isJsonArray()) {
			logSuccess("Type Metadata document has no 'claims' array; no mandatory-presence checks apply");
			return env;
		}
		JsonArray claims = claimsEl.getAsJsonArray();

		List<JsonElement> missing = new ArrayList<>();
		int mandatoryCount = 0;
		for (JsonElement entryEl : claims) {
			if (!entryEl.isJsonObject()) {
				continue;
			}
			JsonObject entry = entryEl.getAsJsonObject();
			JsonElement mandatoryEl = entry.get("mandatory");
			boolean mandatory = mandatoryEl != null
				&& mandatoryEl.isJsonPrimitive()
				&& mandatoryEl.getAsJsonPrimitive().isBoolean()
				&& OIDFJSON.getBoolean(mandatoryEl);
			if (!mandatory) {
				continue;
			}
			JsonElement pathEl = entry.get("path");
			if (pathEl == null || !pathEl.isJsonArray()) {
				continue;
			}
			JsonArray path = pathEl.getAsJsonArray();
			mandatoryCount++;
			if (!allAddressedClaimsArePresent(payload, path)) {
				missing.add(entry);
			}
		}

		if (!missing.isEmpty()) {
			throw error("Type Metadata declares 'mandatory: true' for one or more claims that are not present in the issued credential, violating SD-JWT VC §9.3",
				args("missing_entries", missing, "payload", payload));
		}

		logSuccess("All mandatory-true claims declared by the Type Metadata are present in the issued credential",
			args("mandatory_claims_checked", mandatoryCount));
		return env;
	}

	/**
	 * Applies a §9.1 claim path to the credential and returns true if every
	 * claim it addresses is present and non-null. Returns false if any
	 * addressed claim is missing, null, or if the path cannot be applied to
	 * the underlying JSON shape at any step.
	 */
	private static boolean allAddressedClaimsArePresent(JsonObject root, JsonArray path) {
		List<JsonElement> current = new ArrayList<>();
		current.add(root);
		for (JsonElement segEl : path) {
			if (current.isEmpty()) {
				return false;
			}
			List<JsonElement> next = new ArrayList<>();
			if (segEl.isJsonPrimitive() && segEl.getAsJsonPrimitive().isString()) {
				String key = OIDFJSON.getString(segEl);
				for (JsonElement el : current) {
					if (!el.isJsonObject()) {
						return false;
					}
					JsonElement value = el.getAsJsonObject().get(key);
					if (value == null || value.isJsonNull()) {
						return false;
					}
					next.add(value);
				}
			} else if (segEl.isJsonPrimitive() && segEl.getAsJsonPrimitive().isNumber()) {
				int idx;
				try {
					idx = OIDFJSON.getInt(segEl);
				} catch (RuntimeException e) {
					return false;
				}
				if (idx < 0) {
					return false;
				}
				for (JsonElement el : current) {
					if (!el.isJsonArray()) {
						return false;
					}
					JsonArray arr = el.getAsJsonArray();
					if (idx >= arr.size()) {
						return false;
					}
					JsonElement value = arr.get(idx);
					if (value == null || value.isJsonNull()) {
						return false;
					}
					next.add(value);
				}
			} else if (segEl.isJsonNull()) {
				for (JsonElement el : current) {
					if (!el.isJsonArray()) {
						return false;
					}
					JsonArray arr = el.getAsJsonArray();
					if (arr.isEmpty()) {
						return false;
					}
					for (JsonElement value : arr) {
						if (value == null || value.isJsonNull()) {
							return false;
						}
						next.add(value);
					}
				}
			} else {
				return false;
			}
			current = next;
		}
		return !current.isEmpty();
	}
}
