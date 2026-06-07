package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.Bcp47LocaleValidation;
import net.openid.conformance.util.Bcp47SubtagRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates {@code ui_locales_supported} and {@code claims_locales_supported} in authorization
 * server / OpenID Provider metadata. Each entry must be a well-formed BCP47 language tag with
 * subtags registered in the IANA Language Subtag Registry (via {@link Bcp47SubtagRegistry}).
 *
 * <p>Defined by RFC 8414 section 2 / OpenID Connect Discovery section 3. Duplicates are not
 * spec-forbidden here, so unlike OID4VCI {@code display.locale} validation the uniqueness rule
 * does not apply.
 *
 * <p>Non-canonical casing is conventional BCP47 style but not a validity issue per RFC 5646; it
 * is surfaced as a WARNING by {@link CheckDiscEndpointLocalesCanonicalCasing} instead.
 */
public class CheckDiscEndpointLocalesSyntax extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		List<String> issues = new ArrayList<>();

		validateLocalesArray(env.getElementFromObject("server", "ui_locales_supported"),
			"ui_locales_supported", issues);
		validateLocalesArray(env.getElementFromObject("server", "claims_locales_supported"),
			"claims_locales_supported", issues);

		if (!issues.isEmpty()) {
			throw error("Invalid BCP47 language tag(s) in authorization server metadata",
				args("issues", issues,
					"language_subtag_registry_date", Bcp47SubtagRegistry.getInstance().getFileDate()));
		}

		logSuccess("All ui_locales_supported / claims_locales_supported entries are well-formed BCP47 tags with registered subtags");
		return env;
	}

	private static void validateLocalesArray(JsonElement valuesEl, String fieldName, List<String> issues) {
		if (valuesEl == null) {
			return;
		}
		if (!valuesEl.isJsonArray()) {
			issues.add(String.format("%s: expected JSON array, got %s", fieldName, valuesEl));
			return;
		}
		JsonArray values = valuesEl.getAsJsonArray();
		for (int i = 0; i < values.size(); i++) {
			JsonElement entry = values.get(i);
			if (!OIDFJSON.isString(entry)) {
				issues.add(String.format("%s[%d]: expected string, got %s", fieldName, i, entry));
				continue;
			}
			String tag = OIDFJSON.getString(entry);
			Bcp47LocaleValidation.validateSubtags(tag, String.format("%s[%d]", fieldName, i), issues);
		}
	}
}
