package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWSUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates {@code proof_signing_alg_values_supported} entries inside
 * {@code credential_configurations_supported.*.proof_types_supported}. The {@code jwt}
 * and {@code attestation} proof types are validated here: entries must be asymmetric
 * JWS algorithm names per {@link JWSUtil#isAsymmetricJWSAlgorithm(String)} (OID4VCI 1.0
 * Final Appendix F requires key proofs to use a digital-signature algorithm, so MAC
 * algorithms like {@code HS256} are not acceptable).
 *
 * <p>Every other proof type — including any future proof type the spec adds, typo'd
 * names like {@code "jwwt"}, and {@code cwt} (where the spec requires COSE integer
 * algorithm identifiers but the current schema still types these as strings) — is
 * silently skipped: the unrecognized-type check is the unknown-properties condition's
 * concern, not this one.
 */
public class VCIValidateProofSigningAlgValuesSupported extends AbstractVciCredentialConfigurationsCondition {

	private static final Set<String> JWS_PROOF_TYPES = Set.of("jwt", "attestation");

	@Override
	@PreEnvironment(required = "vci")
	public Environment evaluate(Environment env) {

		List<String> issues = new ArrayList<>();
		forEachCredentialConfiguration(env, (configId, config) -> {
			JsonElement proofTypesEl = config.get("proof_types_supported");
			if (proofTypesEl == null || !proofTypesEl.isJsonObject()) {
				return;
			}
			for (Map.Entry<String, JsonElement> proofEntry : proofTypesEl.getAsJsonObject().entrySet()) {
				String proofType = proofEntry.getKey();
				if (!proofEntry.getValue().isJsonObject()) {
					continue;
				}
				JsonObject proofConfig = proofEntry.getValue().getAsJsonObject();
				JsonElement algValues = proofConfig.get("proof_signing_alg_values_supported");
				if (algValues == null || !algValues.isJsonArray()) {
					continue;
				}
				if (JWS_PROOF_TYPES.contains(proofType)) {
					validateJwsProofSigningAlgs(configId, proofType, algValues.getAsJsonArray(), issues);
				}
				// other proof types (e.g. cwt) are not validated here — see commit message
			}
		});

		if (!issues.isEmpty()) {
			throw error("proof_signing_alg_values_supported contains unrecognized or non-asymmetric algorithm identifier(s)",
				args("issues", issues,
					"valid_asymmetric_jws_algorithms", JWSUtil.validAsymmetricJWSAlgorithms()));
		}

		logSuccess("All proof_signing_alg_values_supported entries (for jwt / attestation proof types) name recognized asymmetric JWS algorithms");
		return env;
	}

	private static void validateJwsProofSigningAlgs(String configId, String proofType, JsonArray algArray, List<String> issues) {
		for (int i = 0; i < algArray.size(); i++) {
			JsonElement element = algArray.get(i);
			String fieldPath = String.format("credential_configurations_supported.%s.proof_types_supported.%s.proof_signing_alg_values_supported[%d]",
				configId, proofType, i);
			if (!OIDFJSON.isString(element)) {
				issues.add(String.format("%s: expected string, got %s", fieldPath, element));
				continue;
			}
			String alg = OIDFJSON.getString(element);
			if (!JWSUtil.isAsymmetricJWSAlgorithm(alg)) {
				if (JWSUtil.isValidJWSAlgorithm(alg)) {
					issues.add(String.format("%s: '%s' is a MAC algorithm; OID4VCI 1.0 Final Appendix F requires key proofs to use an asymmetric digital-signature algorithm",
						fieldPath, alg));
				} else {
					issues.add(String.format("%s: unrecognized JWS algorithm '%s'", fieldPath, alg));
				}
			}
		}
	}
}
