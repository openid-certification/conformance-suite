package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per IETF SD-JWT VC draft-13 §9.4:
 * <ul>
 *   <li>{@code sd: "always"} — the Issuer MUST make the claim selectively disclosable.</li>
 *   <li>{@code sd: "allowed"} — the Issuer MAY make the claim selectively disclosable.</li>
 *   <li>{@code sd: "never"} — the Issuer MUST NOT make the claim selectively disclosable.</li>
 * </ul>
 *
 * Whether a claim was selectively disclosed is determined <em>path-precisely</em>
 * by resolving each §9.1 claim path against the Processed SD-JWT Payload
 * ({@code sdjwt.decoded}) for addressing/presence while walking the raw credential
 * JWT payload ({@code sdjwt.credential.claims}) — which retains the {@code _sd}
 * digest arrays and {@code {"...": digest}} array-element placeholders — for
 * disclosure status. No digest recomputation is needed (the environment stores
 * re-serialized disclosures, not the issuer's original byte sequence). String,
 * non-negative-integer and {@code null} (wildcard) path segments are all
 * supported:
 * <ul>
 *   <li>An object member present in the decoded payload but absent as a literal
 *       member of a raw object that carries an {@code _sd} array was selectively
 *       disclosed.</li>
 *   <li>An array element whose raw position is a {@code {"...": digest}}
 *       placeholder was selectively disclosed; a plain raw element is a clear
 *       claim. Raw and decoded arrays are position-aligned for an issued
 *       credential (every placeholder resolves), so element status maps directly.</li>
 * </ul>
 *
 * <p>Anything that cannot be determined reliably is treated as inconclusive and
 * the constraint is skipped rather than risk a spurious FAILURE: descending past
 * a selectively-disclosed array element (which would require digest matching),
 * descending into a disclosed object whose claim name is shared by more than one
 * disclosure, an unavailable or structurally mismatched raw payload, or a path
 * segment of an unexpected type.
 *
 * <p>When only a parent object is selectively disclosable (e.g. {@code address} as
 * a whole) while a child is a literal member of the disclosure value, the child
 * path is treated as <em>not</em> independently selectively disclosable — a
 * defensible reading of §9.4.
 */
public class VCIEnsureSelectiveDisclosureConformsToTypeMetadata extends AbstractCondition {

	private enum SdStatus { CLEAR, DISCLOSED, INCONCLUSIVE }

	@Override
	@PreEnvironment(required = {"sdjwt", "vci"})
	public Environment evaluate(Environment env) {
		JsonElement claimsEl = env.getElementFromObject("vci", "sdjwt_vc_type_metadata.claims");
		if (claimsEl == null || !claimsEl.isJsonArray()) {
			logSuccess("Type Metadata document has no 'claims' array; no selective-disclosure checks apply");
			return env;
		}
		JsonArray claims = claimsEl.getAsJsonArray();

		JsonObject payload = decodedPayloadOrNull(env);
		JsonObject rawClaims = rawCredentialClaimsOrNull(env);
		Map<String, List<JsonElement>> disclosuresByName = indexThreeElementDisclosuresByName(env);

		List<JsonObject> violations = new ArrayList<>();
		List<JsonObject> skipped = new ArrayList<>();
		int checked = 0;
		for (JsonElement entryEl : claims) {
			if (!entryEl.isJsonObject()) {
				continue;
			}
			JsonObject entry = entryEl.getAsJsonObject();
			JsonElement sdEl = entry.get("sd");
			if (sdEl == null || !sdEl.isJsonPrimitive() || !sdEl.getAsJsonPrimitive().isString()) {
				continue;
			}
			String sd = OIDFJSON.getString(sdEl);
			if ("allowed".equals(sd)) {
				continue;
			}
			JsonElement pathEl = entry.get("path");
			if (pathEl == null || !pathEl.isJsonArray() || pathEl.getAsJsonArray().isEmpty()) {
				continue;
			}
			JsonArray path = pathEl.getAsJsonArray();

			List<SdStatus> statuses = new ArrayList<>();
			collectStatuses(payload, rawClaims, path, 0, disclosuresByName, statuses);
			if (statuses.isEmpty()) {
				// Path addresses no present claim (absent claim or empty array) — sd not applicable.
				continue;
			}
			if (statuses.contains(SdStatus.INCONCLUSIVE)) {
				// Selective-disclosure status could not be determined for one or more
				// addressed claims; skip rather than misjudge.
				skipped.add(entry);
				continue;
			}
			checked++;
			if ("always".equals(sd) && statuses.contains(SdStatus.CLEAR)) {
				violations.add(entry);
			} else if ("never".equals(sd) && statuses.contains(SdStatus.DISCLOSED)) {
				violations.add(entry);
			}
		}

		if (!skipped.isEmpty()) {
			log("Some 'sd' constraints addressed claims whose selective-disclosure status could not be determined from the credential structure and were skipped",
				args("skipped_entries", skipped));
		}

		if (!violations.isEmpty()) {
			throw error("Type Metadata 'sd' constraints are violated by the issued credential, violating SD-JWT VC §9.4",
				args("violations", violations));
		}

		logSuccess("Type Metadata 'sd' constraints are respected by the issued credential",
			args("checked", checked, "skipped", skipped.size()));
		return env;
	}

	/**
	 * Resolves the claim path from segment {@code segIdx} onwards, addressing
	 * claims in {@code decodedParent} and reading their selective-disclosure status
	 * from {@code rawParent}, appending one {@link SdStatus} per addressed (present)
	 * leaf to {@code out}. A path that addresses no present claim contributes
	 * nothing.
	 */
	private static void collectStatuses(JsonElement decodedParent, JsonElement rawParent,
			JsonArray path, int segIdx, Map<String, List<JsonElement>> disclosuresByName, List<SdStatus> out) {
		JsonElement seg = path.get(segIdx);
		boolean isLast = segIdx == path.size() - 1;

		if (seg.isJsonPrimitive() && seg.getAsJsonPrimitive().isString()) {
			String key = OIDFJSON.getString(seg);
			if (decodedParent == null || !decodedParent.isJsonObject()) {
				return;
			}
			JsonElement decodedChild = decodedParent.getAsJsonObject().get(key);
			if (decodedChild == null || decodedChild.isJsonNull()) {
				return;
			}
			SdStatus status;
			JsonElement rawChild;
			if (rawParent != null && rawParent.isJsonObject() && rawParent.getAsJsonObject().has(key)) {
				status = SdStatus.CLEAR;
				rawChild = rawParent.getAsJsonObject().get(key);
			} else if (rawParent != null && rawParent.isJsonObject() && hasSdArray(rawParent.getAsJsonObject())) {
				status = SdStatus.DISCLOSED;
				rawChild = singleDisclosureValue(disclosuresByName, key);
			} else {
				status = SdStatus.INCONCLUSIVE;
				rawChild = null;
			}
			descendOrRecord(decodedChild, rawChild, status, path, segIdx, isLast, disclosuresByName, out);
			return;
		}

		if (seg.isJsonNull()) {
			if (decodedParent == null || !decodedParent.isJsonArray()) {
				return;
			}
			JsonArray decodedArr = decodedParent.getAsJsonArray();
			for (int i = 0; i < decodedArr.size(); i++) {
				addArrayElementStatus(decodedArr, rawParent, i, path, segIdx, isLast, disclosuresByName, out);
			}
			return;
		}

		if (seg.isJsonPrimitive() && seg.getAsJsonPrimitive().isNumber()) {
			int idx;
			try {
				idx = OIDFJSON.getInt(seg);
			} catch (RuntimeException e) {
				out.add(SdStatus.INCONCLUSIVE);
				return;
			}
			if (idx < 0) {
				out.add(SdStatus.INCONCLUSIVE);
				return;
			}
			if (decodedParent == null || !decodedParent.isJsonArray()) {
				return;
			}
			JsonArray decodedArr = decodedParent.getAsJsonArray();
			if (idx >= decodedArr.size()) {
				return;
			}
			addArrayElementStatus(decodedArr, rawParent, idx, path, segIdx, isLast, disclosuresByName, out);
			return;
		}

		// Unexpected path segment type (the structure validator should already have failed it).
		out.add(SdStatus.INCONCLUSIVE);
	}

	private static void addArrayElementStatus(JsonArray decodedArr, JsonElement rawParent, int i,
			JsonArray path, int segIdx, boolean isLast,
			Map<String, List<JsonElement>> disclosuresByName, List<SdStatus> out) {
		JsonElement decodedChild = decodedArr.get(i);
		boolean rawAligned = rawParent != null && rawParent.isJsonArray()
			&& rawParent.getAsJsonArray().size() == decodedArr.size();
		SdStatus status;
		JsonElement rawChild;
		if (!rawAligned) {
			status = SdStatus.INCONCLUSIVE;
			rawChild = null;
		} else {
			JsonElement rawElem = rawParent.getAsJsonArray().get(i);
			if (isArrayElementDisclosurePlaceholder(rawElem)) {
				// Selectively disclosed; descending into it would require digest matching.
				status = SdStatus.DISCLOSED;
				rawChild = null;
			} else {
				status = SdStatus.CLEAR;
				rawChild = rawElem;
			}
		}
		descendOrRecord(decodedChild, rawChild, status, path, segIdx, isLast, disclosuresByName, out);
	}

	private static void descendOrRecord(JsonElement decodedChild, JsonElement rawChild, SdStatus status,
			JsonArray path, int segIdx, boolean isLast,
			Map<String, List<JsonElement>> disclosuresByName, List<SdStatus> out) {
		if (isLast) {
			out.add(status);
		} else if (status == SdStatus.INCONCLUSIVE || rawChild == null) {
			// Cannot navigate further reliably; the addressed leaf is inconclusive.
			out.add(SdStatus.INCONCLUSIVE);
		} else {
			collectStatuses(decodedChild, rawChild, path, segIdx + 1, disclosuresByName, out);
		}
	}

	private static boolean hasSdArray(JsonObject obj) {
		return obj.has("_sd") && obj.get("_sd").isJsonArray();
	}

	private static boolean isArrayElementDisclosurePlaceholder(JsonElement el) {
		if (!el.isJsonObject()) {
			return false;
		}
		JsonObject obj = el.getAsJsonObject();
		return obj.size() == 1 && obj.has("...");
	}

	private static JsonElement singleDisclosureValue(Map<String, List<JsonElement>> disclosuresByName, String key) {
		List<JsonElement> values = disclosuresByName.get(key);
		return (values != null && values.size() == 1) ? values.get(0) : null;
	}

	private static JsonObject decodedPayloadOrNull(Environment env) {
		JsonElement el = env.getElementFromObject("sdjwt", "decoded");
		return (el != null && el.isJsonObject()) ? el.getAsJsonObject() : null;
	}

	private static JsonObject rawCredentialClaimsOrNull(Environment env) {
		JsonElement el = env.getElementFromObject("sdjwt", "credential.claims");
		return (el != null && el.isJsonObject()) ? el.getAsJsonObject() : null;
	}

	/**
	 * Indexes object-property disclosures ({@code [salt, claim_name, value]}) by
	 * claim name, mapping each name to the list of disclosed values declared for
	 * it. A name mapped to more than one value cannot be attributed to a single
	 * structural position when descending.
	 */
	private static Map<String, List<JsonElement>> indexThreeElementDisclosuresByName(Environment env) {
		Map<String, List<JsonElement>> map = new HashMap<>();
		JsonElement disclosuresEl = env.getElementFromObject("sdjwt", "disclosures");
		if (disclosuresEl == null || !disclosuresEl.isJsonArray()) {
			return map;
		}
		for (JsonElement discEl : disclosuresEl.getAsJsonArray()) {
			if (!discEl.isJsonPrimitive() || !discEl.getAsJsonPrimitive().isString()) {
				continue;
			}
			JsonElement parsed;
			try {
				parsed = JsonParser.parseString(OIDFJSON.getString(discEl));
			} catch (JsonSyntaxException e) {
				continue;
			}
			if (!parsed.isJsonArray()) {
				continue;
			}
			JsonArray arr = parsed.getAsJsonArray();
			if (arr.size() != 3) {
				continue;
			}
			JsonElement nameEl = arr.get(1);
			if (nameEl.isJsonPrimitive() && nameEl.getAsJsonPrimitive().isString()) {
				map.computeIfAbsent(OIDFJSON.getString(nameEl), k -> new ArrayList<>()).add(arr.get(2));
			}
		}
		return map;
	}
}
