package net.openid.conformance.util;

import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;

/**
 * Shared BCP47 language-tag validation used by the OID4VCI metadata locale checks
 * (e.g. {@code display.locale}, {@code ui_locales_supported}, {@code claims_locales_supported}).
 * Validates that a tag is well-formed per {@link Locale.Builder#setLanguageTag} and that each of
 * its subtags is registered in the IANA Language Subtag Registry via {@link Bcp47SubtagRegistry}.
 */
public final class Bcp47LocaleValidation {

	private Bcp47LocaleValidation() {
	}

	/**
	 * Validates a single language tag, appending one issue per problem to {@code issues}.
	 *
	 * @param tag    the language tag to validate
	 * @param label  the path/field prefix used in issue messages (everything before the {@code ": "}),
	 *               e.g. {@code "ui_locales_supported[0]"} or {@code "$.display[0].locale"}
	 * @param issues collector for human-readable problem descriptions
	 * @return the canonical form of the tag (for duplicate detection), or {@code null} if the tag is
	 *         malformed (in which case an issue has already been added)
	 */
	public static String validateSubtags(String tag, String label, List<String> issues) {
		Locale locale;
		try {
			locale = new Locale.Builder().setLanguageTag(tag).build();
		} catch (IllformedLocaleException e) {
			issues.add(String.format("%s: '%s' is not a well-formed BCP47 language tag (%s)", label, tag, e.getMessage()));
			return null;
		}
		Bcp47SubtagRegistry registry = Bcp47SubtagRegistry.getInstance();
		String language = locale.getLanguage();
		if (!language.isEmpty() && !registry.isRegisteredLanguage(language)) {
			issues.add(String.format("%s: '%s' contains unregistered language subtag '%s'", label, tag, language));
		}
		String region = locale.getCountry();
		if (!region.isEmpty() && !registry.isRegisteredRegion(region)) {
			issues.add(String.format("%s: '%s' contains unregistered region subtag '%s'", label, tag, region));
		}
		String script = locale.getScript();
		if (!script.isEmpty() && !registry.isRegisteredScript(script)) {
			issues.add(String.format("%s: '%s' contains unregistered script subtag '%s'", label, tag, script));
		}
		String variant = locale.getVariant();
		if (!variant.isEmpty()) {
			for (String v : variant.split("_")) {
				if (!registry.isRegisteredVariant(v.toLowerCase(Locale.ROOT))) {
					issues.add(String.format("%s: '%s' contains unregistered variant subtag '%s'", label, tag, v));
				}
			}
		}
		return locale.toLanguageTag();
	}

	/**
	 * Detects a language tag that differs from its BCP47 canonical form <em>only</em> in case
	 * (e.g. {@code "DE"} vs {@code "de"}, {@code "EN-us"} vs {@code "en-US"}). RFC 5646 §2.1.1
	 * permits any case, so this is a convention/typo concern rather than a validity error.
	 *
	 * @return the canonical-cased form when {@code tag} is well-formed and differs from it only in
	 *         case; {@code null} otherwise. {@code null} is returned for ill-formed tags (their
	 *         well-formedness is a {@link #validateSubtags} concern) and for tags whose canonical
	 *         form differs substantively rather than just in case — e.g. grandfathered tags like
	 *         {@code i-klingon} that {@link Locale#toLanguageTag()} rewrites to {@code tlh}, where
	 *         the change is the tag itself, not its casing.
	 */
	public static String nonCanonicalCasing(String tag) {
		Locale locale;
		try {
			locale = new Locale.Builder().setLanguageTag(tag).build();
		} catch (IllformedLocaleException e) {
			return null;
		}
		String canonical = locale.toLanguageTag();
		if (tag.equals(canonical)) {
			return null;
		}
		if (!tag.toLowerCase(Locale.ROOT).equals(canonical.toLowerCase(Locale.ROOT))) {
			return null;
		}
		return canonical;
	}
}
