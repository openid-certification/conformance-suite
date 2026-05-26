package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Shared traversal of the five {@code display} arrays that can carry {@code locale} fields in
 * OID4VCI credential issuer metadata: the top-level {@code display}, each
 * {@code credential_configurations_supported.*.display}, the top-level
 * {@code credential_configurations_supported.*.claims[].display}, the
 * {@code credential_configurations_supported.*.credential_metadata.display}, and the
 * {@code credential_configurations_supported.*.credential_metadata.claims[].display}.
 *
 * <p>{@link VCIValidateDisplayLocales} (FAILURE for well-formedness / unregistered subtags /
 * duplicates) and {@link VCIWarnOnNonCanonicalDisplayLocales} (WARNING for casing) both walk
 * exactly these arrays. Centralising the walk here keeps the two conditions in lockstep so a
 * future display location added to the spec only has to be wired in once.
 */
final class VciDisplayLocales {

	private VciDisplayLocales() {
	}

	@FunctionalInterface
	interface DisplayArrayVisitor {
		/**
		 * @param displayArray a non-null {@code display} array
		 * @param jsonPath     the JSONPath of {@code displayArray}, e.g. {@code "$.display"}
		 */
		void visit(JsonArray displayArray, String jsonPath);
	}

	/** Invokes {@code visitor} once for each present {@code display} array across all five levels. */
	static void forEachDisplayArray(JsonObject metadata, DisplayArrayVisitor visitor) {
		visitArray(asArrayOrNull(metadata, "display"), "$.display", visitor);

		JsonObject credentialConfigurations = asObjectOrNull(metadata, "credential_configurations_supported");
		if (credentialConfigurations == null) {
			return;
		}
		for (Map.Entry<String, JsonElement> entry : credentialConfigurations.entrySet()) {
			if (!entry.getValue().isJsonObject()) {
				continue;
			}
			String configId = entry.getKey();
			JsonObject config = entry.getValue().getAsJsonObject();

			visitArray(asArrayOrNull(config, "display"),
				String.format("$.credential_configurations_supported.%s.display", configId), visitor);
			visitClaimsDisplay(asArrayOrNull(config, "claims"),
				String.format("$.credential_configurations_supported.%s.claims", configId), visitor);

			JsonObject credentialMetadata = asObjectOrNull(config, "credential_metadata");
			if (credentialMetadata == null) {
				continue;
			}
			visitArray(asArrayOrNull(credentialMetadata, "display"),
				String.format("$.credential_configurations_supported.%s.credential_metadata.display", configId), visitor);
			visitClaimsDisplay(asArrayOrNull(credentialMetadata, "claims"),
				String.format("$.credential_configurations_supported.%s.credential_metadata.claims", configId), visitor);
		}
	}

	private static void visitClaimsDisplay(JsonArray claims, String basePath, DisplayArrayVisitor visitor) {
		if (claims == null) {
			return;
		}
		for (int i = 0; i < claims.size(); i++) {
			JsonElement claim = claims.get(i);
			if (!claim.isJsonObject()) {
				continue;
			}
			visitArray(asArrayOrNull(claim.getAsJsonObject(), "display"),
				String.format("%s[%d].display", basePath, i), visitor);
		}
	}

	private static void visitArray(JsonArray displayArray, String jsonPath, DisplayArrayVisitor visitor) {
		if (displayArray != null) {
			visitor.visit(displayArray, jsonPath);
		}
	}

	static JsonArray asArrayOrNull(JsonObject obj, String member) {
		JsonElement el = obj.get(member);
		return el != null && el.isJsonArray() ? el.getAsJsonArray() : null;
	}

	static JsonObject asObjectOrNull(JsonObject obj, String member) {
		JsonElement el = obj.get(member);
		return el != null && el.isJsonObject() ? el.getAsJsonObject() : null;
	}
}
