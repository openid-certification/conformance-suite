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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Per IETF SD-JWT VC draft-13 §9.4:
 * <ul>
 *   <li>{@code sd: "always"} — the Issuer MUST make the claim selectively disclosable.</li>
 *   <li>{@code sd: "allowed"} — the Issuer MAY make the claim selectively disclosable.</li>
 *   <li>{@code sd: "never"} — the Issuer MUST NOT make the claim selectively disclosable.</li>
 * </ul>
 *
 * Selective-disclosability is determined from {@code sdjwt.disclosures}: each
 * disclosure with three elements ([salt, claim_name, value]) selectively discloses
 * the named object property; two-element disclosures ([salt, value]) selectively
 * disclose an array element and are not addressable by name.
 *
 * For paths of length 1 the leaf name is the claim name; we look it up directly.
 * For longer paths, the leaf name match is ambiguous against the disclosure list
 * (e.g., disclosure {@code given_name} could be a member of any object), so
 * those entries are skipped with an INFO log rather than misjudged. Nested-claim
 * selective-disclosure analysis requires _sd array traversal and is deferred.
 */
public class VCIEnsureSelectiveDisclosureConformsToTypeMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt", "vci"})
	public Environment evaluate(Environment env) {
		JsonElement claimsEl = env.getElementFromObject("vci", "sdjwt_vc_type_metadata.claims");
		if (claimsEl == null || !claimsEl.isJsonArray()) {
			logSuccess("Type Metadata document has no 'claims' array; no selective-disclosure checks apply");
			return env;
		}
		JsonArray claims = claimsEl.getAsJsonArray();

		Set<String> disclosedNames = collectDisclosedClaimNames(env);

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
			if (pathEl == null || !pathEl.isJsonArray()) {
				continue;
			}
			JsonArray path = pathEl.getAsJsonArray();
			if (path.size() != 1
				|| !path.get(0).isJsonPrimitive()
				|| !path.get(0).getAsJsonPrimitive().isString()) {
				skipped.add(entry);
				continue;
			}
			String claimName = OIDFJSON.getString(path.get(0));
			boolean disclosed = disclosedNames.contains(claimName);
			checked++;
			if ("always".equals(sd) && !disclosed) {
				violations.add(entry);
			} else if ("never".equals(sd) && disclosed) {
				violations.add(entry);
			}
		}

		if (!skipped.isEmpty()) {
			log("Some 'sd' constraints apply to multi-segment paths; selective-disclosure analysis for those is not yet implemented and was skipped",
				args("skipped_entries", skipped));
		}

		if (!violations.isEmpty()) {
			throw error("Type Metadata 'sd' constraints are violated by the issued credential, violating SD-JWT VC §9.4",
				args("violations", violations, "disclosed_claim_names", disclosedNames));
		}

		logSuccess("Type Metadata 'sd' constraints are respected by the issued credential",
			args("checked", checked, "skipped_due_to_unsupported_path", skipped.size()));
		return env;
	}

	private static Set<String> collectDisclosedClaimNames(Environment env) {
		Set<String> names = new HashSet<>();
		JsonElement disclosuresEl = env.getElementFromObject("sdjwt", "disclosures");
		if (disclosuresEl == null || !disclosuresEl.isJsonArray()) {
			return names;
		}
		for (JsonElement discEl : disclosuresEl.getAsJsonArray()) {
			if (!discEl.isJsonPrimitive() || !discEl.getAsJsonPrimitive().isString()) {
				continue;
			}
			String discJson = OIDFJSON.getString(discEl);
			JsonElement parsed;
			try {
				parsed = JsonParser.parseString(discJson);
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
				names.add(OIDFJSON.getString(nameEl));
			}
		}
		return names;
	}
}
