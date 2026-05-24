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
 * claim path semantics support strings, nulls, and non-negative integers;
 * paths containing only strings traverse object members. Paths containing
 * {@code null} (wildcard array) or integer (array index) elements are
 * skipped with an INFO log — that traversal is not yet implemented and
 * applying a partial check could give a wrong verdict.
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
		List<JsonElement> skipped = new ArrayList<>();
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
			if (!isStringOnlyPath(path)) {
				skipped.add(entry);
				continue;
			}
			mandatoryCount++;
			if (!resolveStringPath(payload, path)) {
				missing.add(entry);
			}
		}

		if (!skipped.isEmpty()) {
			log("Some mandatory-claim paths contain null or integer elements; presence check for those is not yet implemented and was skipped",
				args("skipped_entries", skipped));
		}

		if (!missing.isEmpty()) {
			throw error("Type Metadata declares 'mandatory: true' for one or more claims that are not present in the issued credential, violating SD-JWT VC §9.3",
				args("missing_entries", missing, "payload", payload));
		}

		logSuccess("All mandatory-true claims declared by the Type Metadata are present in the issued credential",
			args("mandatory_claims_checked", mandatoryCount, "skipped_due_to_unsupported_path", skipped.size()));
		return env;
	}

	private static boolean isStringOnlyPath(JsonArray path) {
		for (JsonElement segEl : path) {
			if (!segEl.isJsonPrimitive() || !segEl.getAsJsonPrimitive().isString()) {
				return false;
			}
		}
		return true;
	}

	private static boolean resolveStringPath(JsonObject root, JsonArray path) {
		JsonElement current = root;
		for (JsonElement segEl : path) {
			if (current == null || !current.isJsonObject()) {
				return false;
			}
			String segment = OIDFJSON.getString(segEl);
			current = current.getAsJsonObject().get(segment);
		}
		return current != null && !current.isJsonNull();
	}
}
