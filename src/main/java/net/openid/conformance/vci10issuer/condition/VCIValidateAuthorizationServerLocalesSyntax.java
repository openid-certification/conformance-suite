package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.Bcp47SubtagRegistry;

import java.util.ArrayList;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;

/**
 * Validates {@code ui_locales_supported} and {@code claims_locales_supported} in
 * authorization server metadata. Each entry must be a well-formed BCP47 language tag
 * in canonical form, with subtags registered in the IANA Language Subtag Registry
 * (via {@link Bcp47SubtagRegistry}).
 *
 * <p>Defined by RFC 8414 section 2 / OpenID Connect Discovery section 3. Unlike the
 * OID4VCI {@code display.locale} validation in {@link VCIValidateDisplayLocales} the
 * uniqueness rule does not apply: duplicates are not spec-forbidden here.
 */
public class VCIValidateAuthorizationServerLocalesSyntax extends AbstractCondition {

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
			if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
				issues.add(String.format("%s[%d]: expected string, got %s", fieldName, i, entry));
				continue;
			}
			String tag = OIDFJSON.getString(entry);
			validateLocale(tag, fieldName, i, issues);
		}
	}

	private static void validateLocale(String tag, String fieldName, int index, List<String> issues) {
		Locale locale;
		try {
			locale = new Locale.Builder().setLanguageTag(tag).build();
		} catch (IllformedLocaleException e) {
			issues.add(String.format("%s[%d]: '%s' is not a well-formed BCP47 language tag (%s)",
				fieldName, index, tag, e.getMessage()));
			return;
		}
		String canonical = locale.toLanguageTag();
		if (!tag.equals(canonical)) {
			issues.add(String.format("%s[%d]: '%s' is not in canonical BCP47 form; expected '%s' (language subtags are lowercase, scripts are Title case, regions are uppercase)",
				fieldName, index, tag, canonical));
			return;
		}
		Bcp47SubtagRegistry registry = Bcp47SubtagRegistry.getInstance();
		String language = locale.getLanguage();
		if (!language.isEmpty() && !registry.isRegisteredLanguage(language)) {
			issues.add(String.format("%s[%d]: '%s' contains unregistered language subtag '%s'",
				fieldName, index, tag, language));
		}
		String region = locale.getCountry();
		if (!region.isEmpty() && !registry.isRegisteredRegion(region)) {
			issues.add(String.format("%s[%d]: '%s' contains unregistered region subtag '%s'",
				fieldName, index, tag, region));
		}
		String script = locale.getScript();
		if (!script.isEmpty() && !registry.isRegisteredScript(script)) {
			issues.add(String.format("%s[%d]: '%s' contains unregistered script subtag '%s'",
				fieldName, index, tag, script));
		}
		String variant = locale.getVariant();
		if (!variant.isEmpty()) {
			for (String v : variant.split("_")) {
				if (!registry.isRegisteredVariant(v.toLowerCase(Locale.ROOT))) {
					issues.add(String.format("%s[%d]: '%s' contains unregistered variant subtag '%s'",
						fieldName, index, tag, v));
				}
			}
		}
	}
}
