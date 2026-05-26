package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.Bcp47LocaleValidation;

import java.util.ArrayList;
import java.util.List;

/**
 * Surfaces {@code ui_locales_supported} / {@code claims_locales_supported} entries whose
 * casing deviates from the BCP47 convention. See {@link VCIWarnOnNonCanonicalDisplayLocales}
 * for the rationale — non-canonical casing is not a validity issue per RFC 5646 §2.1.1
 * but in practice almost always indicates an implementer typo.
 *
 * <p>Wired in separately so the caller can choose WARNING severity; the underlying
 * {@link VCIValidateAuthorizationServerLocalesSyntax} stays a FAILURE for well-formedness
 * and subtag-registry membership.
 */
public class VCIWarnOnNonCanonicalAuthorizationServerLocales extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		List<String> issues = new ArrayList<>();

		walkArray(env.getElementFromObject("server", "ui_locales_supported"), "ui_locales_supported", issues);
		walkArray(env.getElementFromObject("server", "claims_locales_supported"), "claims_locales_supported", issues);

		if (!issues.isEmpty()) {
			throw error("Authorization server locale tags deviate from BCP47 canonical casing (convention is lowercase language subtag, Title-case script, uppercase region); RFC 5646 §2.1.1 permits any case, but real-world implementations typically expect canonical casing.",
				args("issues", issues));
		}

		logSuccess("All ui_locales_supported / claims_locales_supported entries use canonical BCP47 casing");
		return env;
	}

	private static void walkArray(JsonElement valuesEl, String fieldName, List<String> issues) {
		if (valuesEl == null || !valuesEl.isJsonArray()) {
			return;
		}
		JsonArray values = valuesEl.getAsJsonArray();
		for (int i = 0; i < values.size(); i++) {
			JsonElement entry = values.get(i);
			if (!OIDFJSON.isString(entry)) {
				continue;
			}
			String tag = OIDFJSON.getString(entry);
			String canonical = Bcp47LocaleValidation.nonCanonicalCasing(tag);
			if (canonical != null) {
				issues.add(String.format("%s[%d]: '%s' should be '%s' to match BCP47 canonical casing",
					fieldName, i, tag, canonical));
			}
		}
	}
}
