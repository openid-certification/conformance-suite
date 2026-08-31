package net.openid.conformance.statistics;

import net.openid.conformance.variant.VariantSelection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Turns the plan level variant selection stored on a {@code TEST_PLAN} document into one
 * canonical string, so that cube cells can be grouped, compared and filtered by variant
 * without carrying a nested document around.
 *
 * <p>The canonical form is {@code key=value} pairs joined with {@code ;}, sorted by key, so
 * that two plans configured the same way always produce the same string whatever order
 * MongoDB happened to store the fields in. A variant stored in the legacy plain string form
 * (see {@link VariantSelection}) becomes the single pair {@code legacy=<string>}.
 *
 * <p>Variant parameter names and values are derived from Java enum names, so they never
 * contain the separators; a value that does anyway (a legacy string, or corrupt data) has
 * them replaced with {@code _} so that the key still parses back into the same number of
 * pairs it was built from.
 */
public final class VariantKeys {

	/** The parameter name a legacy plain string variant is canonicalised under. */
	public static final String LEGACY = "legacy";

	private static final String PAIR_SEPARATOR = ";";

	private static final String VALUE_SEPARATOR = "=";

	private VariantKeys() {
	}

	/**
	 * @param variant the raw {@code variant} field of a plan document: a {@code Document} or
	 *                any other map, a plain string for a legacy variant, or null
	 * @return the canonical key, or the empty string if there is no variant selection
	 */
	public static String canonical(Object variant) {
		if (variant instanceof CharSequence legacy) {
			String value = legacy.toString().trim();
			return value.isEmpty() ? "" : LEGACY + VALUE_SEPARATOR + sanitise(value);
		}
		if (variant instanceof Map<?, ?> map) {
			return join(pairs(map));
		}
		// anything else - a missing field, a number, an array - is not a variant selection
		return "";
	}

	/**
	 * @param variantKey a key produced by {@link #canonical(Object)}
	 * @return the pairs it is made of, in key order; empty if there is no selection.
	 *         Malformed segments are skipped rather than reported: the key is derived data,
	 *         not something a user typed.
	 */
	public static Map<String, String> parse(String variantKey) {
		if (variantKey == null || variantKey.isEmpty()) {
			return Map.of();
		}
		Map<String, String> pairs = new LinkedHashMap<>();
		for (String pair : variantKey.split(PAIR_SEPARATOR)) {
			int separator = pair.indexOf(VALUE_SEPARATOR);
			if (separator > 0) {
				pairs.put(pair.substring(0, separator), pair.substring(separator + 1));
			}
		}
		return Collections.unmodifiableMap(pairs);
	}

	private static SortedMap<String, String> pairs(Map<?, ?> variant) {
		SortedMap<String, String> pairs = new TreeMap<>();
		for (Map.Entry<?, ?> entry : variant.entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null) {
				continue;
			}
			String name = sanitise(String.valueOf(entry.getKey()).trim());
			if (name.isEmpty()) {
				continue;
			}
			pairs.put(VariantSelection.LEGACY_VARIANT_NAME.equals(name) ? LEGACY : name,
				sanitise(String.valueOf(entry.getValue()).trim()));
		}
		return pairs;
	}

	private static String join(SortedMap<String, String> pairs) {
		return pairs.entrySet().stream()
			.map(pair -> pair.getKey() + VALUE_SEPARATOR + pair.getValue())
			.collect(Collectors.joining(PAIR_SEPARATOR));
	}

	private static String sanitise(String value) {
		return value.replace(PAIR_SEPARATOR, "_").replace(VALUE_SEPARATOR, "_");
	}
}
