package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.Bcp47SubtagRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates {@code locale} fields in every display array within the credential issuer metadata
 * across all five nesting levels (top-level display, per-credential display, top-level
 * dc+sd-jwt claims display, credential_metadata.display, credential_metadata.claims display).
 * Each locale must be:
 * <ul>
 *   <li>a well-formed BCP47 language tag (parseable by {@link Locale.Builder#setLanguageTag});</li>
 *   <li>in canonical form (lowercase language subtag, Title-case script, uppercase region) —
 *       comparing the original to {@link Locale#toLanguageTag()};</li>
 *   <li>composed of subtags registered in the IANA Language Subtag Registry (via
 *       {@link Bcp47SubtagRegistry}), so typos like {@code xx-YY} are caught.</li>
 * </ul>
 * Also enforces the OID4VCI 12.2.4 rule "There MUST be only one object for each language identifier"
 * by detecting duplicate locales within the same display array.
 */
public class VCIValidateDisplayLocales extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();

		List<String> issues = new ArrayList<>();

		validateDisplayArray(metadata.getAsJsonArray("display"), "$.display", issues);

		JsonObject credentialConfigurations = metadata.getAsJsonObject("credential_configurations_supported");
		if (credentialConfigurations != null) {
			for (Map.Entry<String, JsonElement> entry : credentialConfigurations.entrySet()) {
				String configId = entry.getKey();
				if (!entry.getValue().isJsonObject()) {
					continue;
				}
				JsonObject config = entry.getValue().getAsJsonObject();
				validateDisplayArray(config.getAsJsonArray("display"),
					String.format("$.credential_configurations_supported.%s.display", configId), issues);

				JsonArray topLevelClaims = config.getAsJsonArray("claims");
				if (topLevelClaims != null) {
					for (int i = 0; i < topLevelClaims.size(); i++) {
						JsonElement claim = topLevelClaims.get(i);
						if (!claim.isJsonObject()) {
							continue;
						}
						validateDisplayArray(claim.getAsJsonObject().getAsJsonArray("display"),
							String.format("$.credential_configurations_supported.%s.claims[%d].display", configId, i), issues);
					}
				}

				JsonObject credentialMetadata = config.getAsJsonObject("credential_metadata");
				if (credentialMetadata == null) {
					continue;
				}
				validateDisplayArray(credentialMetadata.getAsJsonArray("display"),
					String.format("$.credential_configurations_supported.%s.credential_metadata.display", configId), issues);

				JsonArray claims = credentialMetadata.getAsJsonArray("claims");
				if (claims != null) {
					for (int i = 0; i < claims.size(); i++) {
						JsonElement claim = claims.get(i);
						if (!claim.isJsonObject()) {
							continue;
						}
						validateDisplayArray(claim.getAsJsonObject().getAsJsonArray("display"),
							String.format("$.credential_configurations_supported.%s.credential_metadata.claims[%d].display",
								configId, i), issues);
					}
				}
			}
		}

		if (!issues.isEmpty()) {
			throw error("Display locale issues found in credential issuer metadata",
				args("issues", issues,
					"language_subtag_registry_date", Bcp47SubtagRegistry.getInstance().getFileDate()));
		}

		logSuccess("All display locales are well-formed BCP47 tags with registered subtags and are unique within each display array");
		return env;
	}

	private static void validateDisplayArray(JsonArray displayArray, String jsonPath, List<String> issues) {
		if (displayArray == null) {
			return;
		}
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < displayArray.size(); i++) {
			JsonElement entry = displayArray.get(i);
			if (!entry.isJsonObject()) {
				continue;
			}
			JsonElement localeEl = entry.getAsJsonObject().get("locale");
			if (localeEl == null) {
				continue;
			}
			if (!localeEl.isJsonPrimitive() || !localeEl.getAsJsonPrimitive().isString()) {
				issues.add(String.format("%s[%d].locale: expected string, got %s", jsonPath, i, localeEl));
				continue;
			}
			String tag = OIDFJSON.getString(localeEl);
			validateLocale(tag, jsonPath, i, issues);
			if (!seen.add(tag)) {
				issues.add(String.format("%s[%d].locale: duplicate locale '%s' within this display array — OID4VCI 12.2.4 requires only one object per language identifier",
					jsonPath, i, tag));
			}
		}
	}

	private static void validateLocale(String tag, String jsonPath, int index, List<String> issues) {
		Locale locale;
		try {
			locale = new Locale.Builder().setLanguageTag(tag).build();
		} catch (IllformedLocaleException e) {
			issues.add(String.format("%s[%d].locale: '%s' is not a well-formed BCP47 language tag (%s)",
				jsonPath, index, tag, e.getMessage()));
			return;
		}
		String canonical = locale.toLanguageTag();
		if (!tag.equals(canonical)) {
			issues.add(String.format("%s[%d].locale: '%s' is not in canonical BCP47 form; expected '%s' (language subtags are lowercase, scripts are Title case, regions are uppercase)",
				jsonPath, index, tag, canonical));
			return;
		}
		Bcp47SubtagRegistry registry = Bcp47SubtagRegistry.getInstance();
		String language = locale.getLanguage();
		if (!language.isEmpty() && !registry.isRegisteredLanguage(language)) {
			issues.add(String.format("%s[%d].locale: '%s' contains unregistered language subtag '%s'",
				jsonPath, index, tag, language));
		}
		String region = locale.getCountry();
		if (!region.isEmpty() && !registry.isRegisteredRegion(region)) {
			issues.add(String.format("%s[%d].locale: '%s' contains unregistered region subtag '%s'",
				jsonPath, index, tag, region));
		}
		String script = locale.getScript();
		if (!script.isEmpty() && !registry.isRegisteredScript(script)) {
			issues.add(String.format("%s[%d].locale: '%s' contains unregistered script subtag '%s'",
				jsonPath, index, tag, script));
		}
		String variant = locale.getVariant();
		if (!variant.isEmpty()) {
			for (String v : variant.split("_")) {
				if (!registry.isRegisteredVariant(v.toLowerCase(Locale.ROOT))) {
					issues.add(String.format("%s[%d].locale: '%s' contains unregistered variant subtag '%s'",
						jsonPath, index, tag, v));
				}
			}
		}
	}
}
