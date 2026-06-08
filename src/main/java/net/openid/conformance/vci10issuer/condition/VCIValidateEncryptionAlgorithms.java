package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWEUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ObjIntConsumer;

/**
 * Validates the JWE key-management and content-encryption algorithm names declared in
 * {@code credential_response_encryption} and {@code credential_request_encryption}.
 * Each {@code alg_values_supported} entry (response side only) is checked via
 * {@link JWEUtil#isAsymmetricJWEAlgorithm(String)} — OID4VCI's credential response
 * encryption flow uses a public key advertised by the wallet, so symmetric
 * key-management algorithms like {@code A128KW} or {@code dir} can't interoperate.
 * Each {@code enc_values_supported} entry (both sides) is checked via
 * {@link JWEUtil#isValidEncryptionMethod(String)}. The JSON schema enforces type only,
 * so unregistered names like {@code "RSA-OAEPxx"} or {@code "A128GCMxx"} pass schema
 * validation and are caught here.
 */
public class VCIValidateEncryptionAlgorithms extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		JsonObject metadata = env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject();

		List<String> issues = new ArrayList<>();
		validateBlock(metadata, "credential_response_encryption", true, issues);
		validateBlock(metadata, "credential_request_encryption", false, issues);

		if (!issues.isEmpty()) {
			throw error("Unrecognized or non-asymmetric JWE algorithm or encryption method in credential issuer metadata",
				args("issues", issues,
					"valid_asymmetric_jwe_algorithms", JWEUtil.validAsymmetricJWEAlgorithms(),
					"valid_encryption_methods", JWEUtil.validEncryptionMethods()));
		}

		logSuccess("All credential_request_encryption / credential_response_encryption alg and enc values name recognized algorithms");
		return env;
	}

	private static void validateBlock(JsonObject metadata, String blockName, boolean expectAlgValues, List<String> issues) {
		JsonElement blockEl = metadata.get(blockName);
		if (blockEl == null || !blockEl.isJsonObject()) {
			return;
		}
		JsonObject block = blockEl.getAsJsonObject();

		if (expectAlgValues) {
			validateStringArray(block.get("alg_values_supported"), blockName, "alg_values_supported", issues, (alg, i) -> {
				if (!JWEUtil.isAsymmetricJWEAlgorithm(alg)) {
					if (JWEUtil.isValidJWEAlgorithm(alg)) {
						issues.add(String.format("%s.alg_values_supported[%d]: '%s' is a symmetric JWE algorithm; credential response encryption uses the wallet's published public key and requires an asymmetric key-management algorithm",
							blockName, i, alg));
					} else {
						issues.add(String.format("%s.alg_values_supported[%d]: unrecognized JWE algorithm '%s'", blockName, i, alg));
					}
				}
			});
		}
		validateStringArray(block.get("enc_values_supported"), blockName, "enc_values_supported", issues, (enc, i) -> {
			if (!JWEUtil.isValidEncryptionMethod(enc)) {
				issues.add(String.format("%s.enc_values_supported[%d]: unrecognized JWE encryption method '%s'", blockName, i, enc));
			}
		});
	}

	/**
	 * Walks a metadata string array, flagging any non-string entry, and passes each string value
	 * (with its index) to {@code validator} for the field-specific algorithm check.
	 */
	private static void validateStringArray(JsonElement valuesEl, String blockName, String fieldName,
			List<String> issues, ObjIntConsumer<String> validator) {
		if (valuesEl == null || !valuesEl.isJsonArray()) {
			return;
		}
		JsonArray values = valuesEl.getAsJsonArray();
		for (int i = 0; i < values.size(); i++) {
			JsonElement entry = values.get(i);
			if (!OIDFJSON.isString(entry)) {
				issues.add(String.format("%s.%s[%d]: expected string, got %s", blockName, fieldName, i, entry));
				continue;
			}
			validator.accept(OIDFJSON.getString(entry), i);
		}
	}
}
