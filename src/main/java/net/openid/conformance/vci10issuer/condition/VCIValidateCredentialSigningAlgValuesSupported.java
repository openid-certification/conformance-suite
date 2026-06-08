package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.CoseAlgorithmUtil;
import net.openid.conformance.util.JWSUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that each entry in {@code credential_signing_alg_values_supported} names a real
 * algorithm. JWS algorithm strings are checked via {@link JWSUtil#isValidJWSAlgorithm(String)};
 * COSE integer identifiers (used by {@code mso_mdoc}) are checked via
 * {@link CoseAlgorithmUtil#isValidCoseSignatureAlgorithm(int)}. The JSON schema only enforces
 * structural type, so typos like {@code "ES265"} pass schema validation and are caught here.
 */
public class VCIValidateCredentialSigningAlgValuesSupported extends AbstractVciCredentialConfigurationsCondition {

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		List<String> issues = new ArrayList<>();
		forEachCredentialConfiguration(env, (configId, config) -> {
			JsonElement algValues = config.get("credential_signing_alg_values_supported");
			if (algValues == null || !algValues.isJsonArray()) {
				return;
			}
			JsonElement formatEl = config.get("format");
			if (!OIDFJSON.isString(formatEl)) {
				return;
			}
			String format = OIDFJSON.getString(formatEl);
			JsonArray algArray = algValues.getAsJsonArray();
			switch (format) {
				case "dc+sd-jwt", "jwt_vc_json" -> validateStringAlgArray(configId, format, algArray, issues);
				case "mso_mdoc" -> validateCoseAlgArray(configId, algArray, issues);
				default -> {
					// other formats are validated by separate format-specific conditions
				}
			}
		});

		if (!issues.isEmpty()) {
			throw error("credential_signing_alg_values_supported contains unrecognized or non-asymmetric algorithm identifier(s)",
				args("issues", issues,
					"valid_asymmetric_jws_algorithms", JWSUtil.validAsymmetricJWSAlgorithms(),
					"valid_cose_signature_alg_ids", CoseAlgorithmUtil.validCoseSignatureAlgorithmIds()));
		}

		logSuccess("All credential_signing_alg_values_supported entries name recognized asymmetric algorithms");
		return env;
	}

	private static void validateStringAlgArray(String configId, String format, JsonArray algArray, List<String> issues) {
		for (JsonElement element : algArray) {
			if (!OIDFJSON.isString(element)) {
				issues.add(String.format("credential_configurations_supported.%s (%s): expected string algorithm name, got %s",
					configId, format, element));
				continue;
			}
			String alg = OIDFJSON.getString(element);
			if (!JWSUtil.isAsymmetricJWSAlgorithm(alg)) {
				if (JWSUtil.isValidJWSAlgorithm(alg)) {
					issues.add(String.format("credential_configurations_supported.%s (%s): '%s' is a MAC algorithm; the wallet verifies the credential signature with the issuer's published public key so an asymmetric digital-signature algorithm is required",
						configId, format, alg));
				} else {
					issues.add(String.format("credential_configurations_supported.%s (%s): unrecognized JWS algorithm '%s'",
						configId, format, alg));
				}
			}
		}
	}

	private static void validateCoseAlgArray(String configId, JsonArray algArray, List<String> issues) {
		for (JsonElement element : algArray) {
			if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
				issues.add(String.format("credential_configurations_supported.%s (mso_mdoc): expected COSE integer algorithm identifier, got %s",
					configId, element));
				continue;
			}
			Number coseAlg = OIDFJSON.getNumber(element);
			if (coseAlg.doubleValue() != coseAlg.longValue()) {
				issues.add(String.format("credential_configurations_supported.%s (mso_mdoc): COSE algorithm identifier must be an integer, got %s",
					configId, element));
				continue;
			}
			int coseAlgId = coseAlg.intValue();
			if (!CoseAlgorithmUtil.isValidCoseSignatureAlgorithm(coseAlgId)) {
				issues.add(String.format("credential_configurations_supported.%s (mso_mdoc): unrecognized COSE signature algorithm id %d",
					configId, coseAlgId));
			}
		}
	}
}
