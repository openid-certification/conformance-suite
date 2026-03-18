package net.openid.conformance.variant;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Map;
import java.util.stream.Collectors;

public class VariantSelection {

	// We need to retain support for legacy (plain string) variants here, as
	// old tests in the database need to be readable.

	public static final String LEGACY_VARIANT_NAME = "__variant__";

	private final Map<String, String> variant;

	public static final VariantSelection EMPTY = new VariantSelection(Map.of());

	public static VariantSelection fromJson(JsonElement json) {
		if (json == null || json.isJsonNull() || json.equals(new JsonPrimitive(""))) {
			return new VariantSelection(Map.of());
		} else if (json.isJsonPrimitive()) {
			return new VariantSelection(OIDFJSON.getString(json));
		} else if (json.isJsonObject()) {
			// Fix the types
			return new VariantSelection(
					json.getAsJsonObject().entrySet().stream()
					.collect(Collectors.toMap(
							e -> e.getKey(),
							e -> OIDFJSON.getString(e.getValue()))));
		} else {
			throw new IllegalArgumentException("Invalid variant selection: " + json);
		}
	}

	@Override
	public String toString() {
		return "VariantSelection{" +
			"variant=" + variant +
			'}';
	}

	public static VariantParameter resolveVariantParameter(Class<?> parameterClass) {
		if (!parameterClass.isEnum()) {
			throw new IllegalArgumentException("Variant parameters must be enums: " + parameterClass.getName());
		}
		VariantParameter annotation = parameterClass.getAnnotation(VariantParameter.class);
		if (annotation == null) {
			throw new IllegalArgumentException("Class does not have a VariantParameter annotation: " + parameterClass.getName());
		}
		return annotation;
	}

	public String getVariantParameterValue(Class<?> parameterClass) {
		String variantParameterName = resolveVariantParameter(parameterClass).name();
		return getVariant().get(variantParameterName);
	}

	public VariantSelection(Map<String, String> variant) {
		this.variant = variant;
	}

	public VariantSelection(String legacyVariant) {
		this.variant = Map.of(LEGACY_VARIANT_NAME, legacyVariant);
	}

	public boolean isLegacyVariant() {
		return variant.containsKey(LEGACY_VARIANT_NAME);
	}

	public Map<String, String> getVariant() {
		return variant;
	}

	public String getVariantAsKeyPairString() {
		if(variant != null) {
			return variant.entrySet()
				.stream()
				.map(e -> e.getKey() + "=" + e.getValue())
				.collect(Collectors.joining(", "));
		} else {
			return "";
		}
	}


	public String getLegacyVariant() {
		return variant.get(LEGACY_VARIANT_NAME);
	}

}
