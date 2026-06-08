package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.Bcp47LocaleValidation;
import net.openid.conformance.util.Bcp47SubtagRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Validates {@code locale} fields in every display array within the credential issuer metadata
 * across all five nesting levels (top-level display, per-credential display, top-level
 * dc+sd-jwt claims display, credential_metadata.display, credential_metadata.claims display).
 * Each locale must be:
 * <ul>
 *   <li>a well-formed BCP47 language tag (parseable by {@link Locale.Builder#setLanguageTag});</li>
 *   <li>composed of subtags registered in the IANA Language Subtag Registry (via
 *       {@link Bcp47SubtagRegistry}), so typos like {@code xx-YY} are caught.</li>
 * </ul>
 * Also enforces the OID4VCI 12.2.4 rule "There MUST be only one object for each language identifier"
 * by detecting duplicate locales within the same display array. Duplicate detection compares
 * canonical forms (RFC 5646 §2.1.1 says case-sensitive matching is not permitted), so
 * {@code "DE"} and {@code "de"} are treated as the same locale here.
 *
 * <p>Non-canonical casing is conventional BCP47 style but not a validity issue per RFC 5646;
 * it is surfaced as a WARNING by {@code VCIWarnOnNonCanonicalDisplayLocales} instead.
 */
public class VCIValidateDisplayLocales extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();

		List<String> issues = new ArrayList<>();

		VciDisplayLocales.forEachDisplayArray(metadata,
			(displayArray, jsonPath) -> validateDisplayArray(displayArray, jsonPath, issues));

		if (!issues.isEmpty()) {
			throw error("Display locale issues found in credential issuer metadata",
				args("issues", issues,
					"language_subtag_registry_date", Bcp47SubtagRegistry.getInstance().getFileDate()));
		}

		logSuccess("All display locales are well-formed BCP47 tags with registered subtags and are unique within each display array");
		return env;
	}

	private static void validateDisplayArray(JsonArray displayArray, String jsonPath, List<String> issues) {
		Map<String, String> seen = new HashMap<>();
		for (int i = 0; i < displayArray.size(); i++) {
			JsonElement entry = displayArray.get(i);
			if (!entry.isJsonObject()) {
				continue;
			}
			JsonElement localeEl = entry.getAsJsonObject().get("locale");
			if (localeEl == null) {
				continue;
			}
			if (!OIDFJSON.isString(localeEl)) {
				issues.add(String.format("%s[%d].locale: expected string, got %s", jsonPath, i, localeEl));
				continue;
			}
			String tag = OIDFJSON.getString(localeEl);
			String dedupKey = Bcp47LocaleValidation.validateSubtags(tag, String.format("%s[%d].locale", jsonPath, i), issues);
			if (dedupKey != null) {
				String previousTag = seen.putIfAbsent(dedupKey, tag);
				if (previousTag != null) {
					issues.add(String.format("%s[%d].locale: duplicate locale '%s' within this display array (canonical form '%s' already used by earlier entry with locale '%s') — OID4VCI 12.2.4 requires only one object per language identifier",
						jsonPath, i, tag, dedupKey, previousTag));
				}
			}
		}
	}
}
