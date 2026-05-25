package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates the {@code zip_values_supported} entries declared in
 * {@code credential_response_encryption} and {@code credential_request_encryption}.
 * RFC 7516 §4.1.3 registers only {@code DEF} as a JWE compression algorithm, so any
 * other value is unlikely to interoperate. Surfaces all non-{@code DEF} values as
 * an error; the caller wires this in at {@code ConditionResult.WARNING} severity.
 */
public class VCIValidateEncryptionZipValues extends AbstractCondition {

	private static final Set<String> REGISTERED_ZIP_VALUES = Set.of("DEF");

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();

		List<String> issues = new ArrayList<>();
		validateBlock(metadata, "credential_response_encryption", issues);
		validateBlock(metadata, "credential_request_encryption", issues);

		if (!issues.isEmpty()) {
			throw error("Unregistered zip_values_supported entry in credential issuer metadata; "
				+ "RFC 7516 section 4.1.3 currently registers only 'DEF'",
				args("issues", issues,
					"registered_zip_values", REGISTERED_ZIP_VALUES));
		}

		logSuccess("All credential_request_encryption / credential_response_encryption zip_values_supported entries are registered");
		return env;
	}

	private static void validateBlock(JsonObject metadata, String blockName, List<String> issues) {
		JsonElement blockEl = metadata.get(blockName);
		if (blockEl == null || !blockEl.isJsonObject()) {
			return;
		}
		JsonElement valuesEl = blockEl.getAsJsonObject().get("zip_values_supported");
		if (valuesEl == null || !valuesEl.isJsonArray()) {
			return;
		}
		JsonArray values = valuesEl.getAsJsonArray();
		for (int i = 0; i < values.size(); i++) {
			JsonElement entry = values.get(i);
			if (!OIDFJSON.isString(entry)) {
				issues.add(String.format("%s.zip_values_supported[%d]: expected string, got %s", blockName, i, entry));
				continue;
			}
			String zip = OIDFJSON.getString(entry);
			if (!REGISTERED_ZIP_VALUES.contains(zip)) {
				issues.add(String.format("%s.zip_values_supported[%d]: '%s' is not a registered JWE compression algorithm",
					blockName, i, zip));
			}
		}
	}
}
