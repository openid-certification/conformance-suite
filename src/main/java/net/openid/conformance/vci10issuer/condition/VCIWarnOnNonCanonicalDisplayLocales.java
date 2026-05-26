package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.Bcp47LocaleValidation;

import java.util.ArrayList;
import java.util.List;

/**
 * Surfaces display {@code locale} fields whose casing deviates from the BCP47 convention
 * (lowercase language subtag, Title-case script, uppercase region). RFC 5646 §2.1.1
 * explicitly says "tags are not case sensitive (and case-sensitive matching is not
 * permitted)", so non-canonical casing is not a validity issue — but in practice it
 * is almost always an implementer typo (e.g. {@code "DE"} when they meant {@code "de"}).
 *
 * <p>Walks the same five display arrays as {@link VCIValidateDisplayLocales} via the shared
 * {@link VciDisplayLocales} traversal, and uses {@link Bcp47LocaleValidation#nonCanonicalCasing}
 * so casing detection (including the grandfathered-tag exclusion) is shared with the other
 * locale conditions. Wired in separately so the caller can choose WARNING severity; the
 * underlying check at {@code VCIValidateDisplayLocales} stays a FAILURE for well-formedness and
 * subtag-registry membership.
 */
public class VCIWarnOnNonCanonicalDisplayLocales extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();

		List<String> issues = new ArrayList<>();

		VciDisplayLocales.forEachDisplayArray(metadata, (displayArray, jsonPath) -> {
			for (int i = 0; i < displayArray.size(); i++) {
				JsonElement entry = displayArray.get(i);
				if (!entry.isJsonObject()) {
					continue;
				}
				JsonElement localeEl = entry.getAsJsonObject().get("locale");
				if (!OIDFJSON.isString(localeEl)) {
					continue;
				}
				String tag = OIDFJSON.getString(localeEl);
				String canonical = Bcp47LocaleValidation.nonCanonicalCasing(tag);
				if (canonical != null) {
					issues.add(String.format("%s[%d].locale: '%s' should be '%s' to match BCP47 canonical casing",
						jsonPath, i, tag, canonical));
				}
			}
		});

		if (!issues.isEmpty()) {
			throw error("Display locale tags deviate from BCP47 canonical casing (convention is lowercase language subtag, Title-case script, uppercase region); RFC 5646 §2.1.1 permits any case, but real-world implementations typically expect canonical casing.",
				args("issues", issues));
		}

		logSuccess("All display locale tags use canonical BCP47 casing");
		return env;
	}
}
